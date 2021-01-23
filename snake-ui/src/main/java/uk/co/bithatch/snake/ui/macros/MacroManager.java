package uk.co.bithatch.snake.ui.macros;

import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.regex.Pattern;

import com.sshtools.twoslices.Toast;
import com.sshtools.twoslices.ToastType;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.macrolib.AbstractUInputMacroDevice;
import uk.co.bithatch.macrolib.ActionBinding;
import uk.co.bithatch.macrolib.ActionMacro;
import uk.co.bithatch.macrolib.CommandMacro;
import uk.co.bithatch.macrolib.Macro;
import uk.co.bithatch.macrolib.MacroBank;
import uk.co.bithatch.macrolib.MacroDevice;
import uk.co.bithatch.macrolib.MacroProfile;
import uk.co.bithatch.macrolib.MacroSystem;
import uk.co.bithatch.macrolib.MacroSystem.ActiveBankListener;
import uk.co.bithatch.macrolib.MacroSystem.ActiveProfileListener;
import uk.co.bithatch.macrolib.MacroSystem.ProfileListener;
import uk.co.bithatch.macrolib.MacroSystem.RecordingListener;
import uk.co.bithatch.macrolib.RecordingSession;
import uk.co.bithatch.macrolib.RecordingState;
import uk.co.bithatch.macrolib.ScriptMacro;
import uk.co.bithatch.macrolib.SimpleMacro;
import uk.co.bithatch.macrolib.TargetType;
import uk.co.bithatch.macrolib.UInputMacro;
import uk.co.bithatch.snake.lib.Backend.BackendListener;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.ValidationException;
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.DeviceLayout;
import uk.co.bithatch.snake.lib.layouts.DeviceView;
import uk.co.bithatch.snake.lib.layouts.IO;
import uk.co.bithatch.snake.lib.layouts.Key;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.util.Language;

/**
 * This is the bridge between Snake and Macrolib. It converts between native
 * Snakelib devices and Macrolib Macro devices, and provides other convenience
 * methods.
 * <p>
 * It is hoped this will be kept light, as most of the logic is in
 * {@link MacroSytem}.
 */
public class MacroManager implements AutoCloseable, ActiveBankListener, ActiveProfileListener, ProfileListener,
		BackendListener, RecordingListener {
	final static Logger LOG = System.getLogger(MacroManager.class.getName());

	final static ResourceBundle bundle = ResourceBundle.getBundle(MacroManager.class.getName());

	public static final String CLIENT_PROP_INCLUDE_INPUT_EVENT_DEVICES = "includeInputEventDevices";
	public static final String CLIENT_PROP_EXCLUDE_INPUT_EVENT_DEVICES = "excludeInputEventDevices";

	private MacroSystem macroSystem;
	private App context;
	private Map<MacroDevice, Device> macroDevices = new HashMap<>();
	private Map<Device, MacroDevice> reverseMacroDevices = new HashMap<>();
	private Map<String, List<Pattern>> includeDevicePatterns = new HashMap<>();
	private Map<String, List<Pattern>> excludeDevicePatterns = new HashMap<>();

	private boolean started;

	public MacroManager(App context) {
		super();
		this.context = context;
		macroSystem = new MacroSystem(new MacrolibStorage(context));
	}

	public void start() throws Exception {
		if ("false".equals(System.getProperty("snake.macrolib", "true"))) {
			LOG.log(Level.WARNING, "Macrolib disabled by system property snake.macrolib.");
			return;
		}

		for (Device device : context.getBackend().getDevices()) {
			DeviceLayout deviceLayout = context.getLayouts().getLayout(device);
			if (deviceLayout != null) {
				addPatternsForDevice(deviceLayout, device);
			}
		}

		/*
		 * Need to match the OpenRazer device to the input device file. Unfortunately,
		 * OpenRazer doesnt give us this information so we must get it ourselves.
		 * 
		 * We do this by matching the end of the device name as OpenRazer provides it
		 * with the device name as reported by the linux input system. The latter
		 * appears the be the former, prefixed with 'Razer '.
		 */
//		var devPaths = new HashMap<String, Path>();
//		for (InputDevice dev : InputDevice.getAvailableDevices()) {
//			if (dev.getName().startsWith("Razer ")) {
//				var name = dev.getName().substring(6);
//				var file = dev.getFile();
//				devPaths.put(name, file);
//				LOG.log(Level.INFO, String.format("Found Razer input device %s at %s.", name, file));
//			}
//		}

		/*
		 * Check /dev/uinput now. If we grab the devies without being able to retarget
		 * the events, effectively the mouse and/or keyboard could stop working entirely
		 * until Snake is stopped.
		 */
		Path uinputPath = Paths.get("/dev/uinput");
		if (!Files.isWritable(uinputPath) || !Files.isReadable(uinputPath)) {
			LOG.log(Level.WARNING,
					String.format("%s is not readable and writable. Macrolib cannot initialise.", uinputPath));
		} else {
			for (Device device : context.getBackend().getDevices()) {
				addMacroDevice(device);
			}
			macroSystem.open();
			macroSystem.addActiveBankListener(this);
			macroSystem.addActiveProfileListener(this);
			macroSystem.addProfileListener(this);
			macroSystem.addRecordingListener(this);
			started = true;
		}

		context.getBackend().addListener(this);
	}

	protected void addMacroDevice(Device device) throws IOException {
		// var path = devPaths.get(device.getName());
		var paths = findDeviceInputPaths(device.getName());
		if (paths == null)
			LOG.log(Level.WARNING, String.format("No input device path found for razer device %s.", device.getName()));
		else {
			for (Path path : paths) {
				LOG.log(Level.INFO,
						String.format("Open input device file %s for Razer device %s.", device.getName(), path));

				try {
					AbstractUInputMacroDevice macroDevice = new AbstractUInputMacroDevice(path) {

						@Override
						public String getUID() {
							return device.getSerial();
						}

						@Override
						public TargetType getJoystickMode() {
							return TargetType.DIGITAL_JOYSTICK;
						}

						@Override
						public int getJoystickCalibration() {
							return 0;
						}

						@Override
						public String getId() {
							return device.getName();
						}

						@Override
						public int getBanks() {
							if (device.getCapabilities().contains(Capability.DEDICATED_MACRO_KEYS))
								return 7;
							else
								return 3;
						}

						@Override
						public Map<String, ActionBinding> getActionKeys() {
							return Collections.emptyMap();
						}

						@Override
						public Collection<EventCode> getSupportedInputEvents() {
							if (context.getLayouts().hasLayout(device)) {
								DeviceLayout layout = context.getLayouts().getLayout(device);
								List<DeviceView> views = layout.getViewsThatHave(ComponentType.KEY);
								Set<EventCode> codes = new HashSet<>();
								for (DeviceView v : views) {
									for (IO io : v.getElements(ComponentType.KEY)) {
										Key key = (Key) io;
										if (key.getEventCode() != null)
											codes.add(key.getEventCode());
									}
								}
								if (!codes.isEmpty())
									return codes;
							}
							return super.getSupportedInputEvents();
						}
					};
					macroSystem.addDevice(macroDevice);
					macroDevices.put(macroDevice, device);
					reverseMacroDevices.put(device, macroDevice);
					setLEDsForDevice(macroDevice);
				} catch (Exception e) {
					if (LOG.isLoggable(Level.DEBUG))
						LOG.log(Level.ERROR, String.format("Failed to add device %s to macro system.", path), e);
					else
						LOG.log(Level.WARNING, String.format("Failed to add device to macro system %s.", path), e);
				}
			}
		}
	}

	protected void addPatternsForDevice(DeviceLayout deviceLayout, Device device) {
		addPatternsForDevice(includeDevicePatterns, CLIENT_PROP_INCLUDE_INPUT_EVENT_DEVICES, deviceLayout, device,
				true);
		addPatternsForDevice(excludeDevicePatterns, CLIENT_PROP_EXCLUDE_INPUT_EVENT_DEVICES, deviceLayout, device,
				false);
	}

	protected void addPatternsForDevice(Map<String, List<Pattern>> map, String key, DeviceLayout deviceLayout,
			Device device, boolean warnEmpty) {
		List<String> patterns = deviceLayout.getClientProperty(key, Collections.emptyList());
		if (patterns.isEmpty()) {
			if (warnEmpty && !deviceLayout.getViewsThatHave(ComponentType.KEY).isEmpty())
				LOG.log(Level.WARNING, String.format(
						"Device %s has a layout, but the layout does not contain a client property of '%s'. This means input devices cannot be determined. Please report this to the Snake project.",
						device.getName(), key));
		} else {
			for (String pattern : patterns) {
				addDevicePattern(map, Pattern.compile(pattern), device.getName());
			}
		}
	}

	protected void addDevicePattern(Map<String, List<Pattern>> map, Pattern pattern, String key) {
		List<Pattern> l = map.get(key);
		if (l == null) {
			l = new ArrayList<>();
			map.put(key, l);
		}
		l.add(pattern);
	}

	@Override
	public void close() throws Exception {
		context.getBackend().removeListener(this);
		macroSystem.removeActiveBankListener(this);
		macroSystem.removeActiveProfileListener(this);
		macroSystem.removeProfileListener(this);
		try {
			macroSystem.close();
		} finally {
			started = false;
		}
	}

	/**
	 * Get a native device given the macro system device.
	 * 
	 * @param device macro system device
	 * @return native device
	 */
	public Device getNativeDevice(MacroDevice device) {
		return macroDevices.get(device);
	}

	/**
	 * Get a macro system device given a native device.
	 * 
	 * @param device native device
	 * @return macro system device
	 */
	public MacroDevice getMacroDevice(Device device) {
		return reverseMacroDevices.get(device);
	}

	/**
	 * Get the underlying macro system instance provided by macrolib.
	 * 
	 * @return macro system
	 */
	public MacroSystem getMacroSystem() {
		return macroSystem;
	}

	/**
	 * Get if this device supports macros.
	 * 
	 * @param device device
	 */
	public boolean isSupported(Device device) {
		return getMacroDevice(device) != null;
	}

	public boolean isStarted() {
		return started;
	}

	public void validate(Macro mkey) throws ValidationException {
		/* TODO: perform validation similar to other macro implmentations */
		if (mkey instanceof UInputMacro) {
			UInputMacro uinput = (UInputMacro) mkey;
			if (uinput.getCode() == null)
				throw new ValidationException("error.missingUInputCode");
		} else if (mkey instanceof SimpleMacro) {
			SimpleMacro simple = (SimpleMacro) mkey;
			if (simple.getMacro() == null || simple.getMacro().length() == 0)
				throw new ValidationException("error.missingSimpleMacro");
		} else if (mkey instanceof ScriptMacro) {
			ScriptMacro script = (ScriptMacro) mkey;
			if (script.getScript() == null || script.getScript().isEmpty())
				throw new ValidationException("error.missingScript");
		} else if (mkey instanceof CommandMacro) {
			CommandMacro command = (CommandMacro) mkey;
			if (command.getCommand() == null || command.getCommand().length() == 0)
				throw new ValidationException("error.missingCommand");
		} else if (mkey instanceof ActionMacro) {
			ActionMacro script = (ActionMacro) mkey;
			if (script.getAction() == null || script.getAction().length() == 0)
				throw new ValidationException("error.missingAction");
		}

	}

	@Override
	public void activeBankChanged(MacroDevice device, MacroBank bank) {
		String cpath = getDeviceImagePath(device);
		Toast.toast(ToastType.INFO, cpath,
				MessageFormat.format(bundle.getString("bankChange.title"), bank.getDisplayName(),
						bank.getProfile().getName()),
				MessageFormat.format(bundle.getString("bankChange"), bank.getDisplayName(),
						bank.getProfile().getName()));
		setLEDsForDevice(device);
	}

	protected String getDeviceImagePath(MacroDevice device) {
		Device nativeDevice = getNativeDevice(device);
		String cpath = context.getCache()
				.getCachedImage(context.getDefaultImage(nativeDevice.getType(), nativeDevice.getImage()));
		if (cpath.startsWith("file:"))
			cpath = cpath.substring(5);
		return cpath;
	}

	@Override
	public void activeProfileChanged(MacroDevice device, MacroProfile profile) {
		String cpath = getDeviceImagePath(device);
		MacroBank activeBank = macroSystem.getActiveBank(device);
		Toast.toast(ToastType.INFO, cpath,
				MessageFormat.format(bundle.getString("profileChange.title"), profile.getName(),
						activeBank.getDisplayName()),
				MessageFormat.format(bundle.getString("profileChange"), profile.getName(),
						activeBank.getDisplayName()));
		setLEDsForDevice(device);
	}

	@Override
	public void profileChanged(MacroDevice device, MacroProfile profile) {
		if (profile.isActive()) {
			setLEDsForDevice(device);
		}

	}

	protected Collection<Path> findDeviceInputPaths(String name) throws IOException {
		List<Pattern> includePatterns = includeDevicePatterns.get(name);
		if (includePatterns == null)
			return null;
		List<Pattern> excludePatterns = excludeDevicePatterns.get(name);
		List<Path> paths = new ArrayList<>();
		for (Path p : Files.newDirectoryStream(Paths.get("/dev/input/by-id"), (f) -> matches(includePatterns, f)
				&& (excludePatterns == null || excludePatterns.isEmpty() || !matches(excludePatterns, f))))
			paths.add(p);
		return paths;
	}

	protected boolean matches(List<Pattern> patterns, Path f) {
		for (Pattern pattern : patterns) {
			if (pattern.matcher(f.getFileName().toString()).matches())
				return true;
		}
		return false;
	}

	protected void setLEDsForDevice(MacroDevice device) {
		Device nativeDevice = getNativeDevice(device);
		if (nativeDevice.getCapabilities().contains(Capability.PROFILE_LEDS)) {
			MacroBank bank = macroSystem.getActiveBank(device);
			@SuppressWarnings("unchecked")
			List<Boolean> l = (List<Boolean>) bank.getProperties().getOrDefault("leds", null);
			if (l == null)
				nativeDevice.setProfileRGB(Language.toBinaryArray(bank.getBank() + 1, 3));
			else
				nativeDevice.setProfileRGB(new boolean[] { l.get(0), l.get(1), l.get(2) });
		}
	}

	@Override
	public void deviceAdded(Device device) {
		DeviceLayout deviceLayout = context.getLayouts().getLayout(device);
		if (deviceLayout != null) {
			addPatternsForDevice(deviceLayout, device);
			try {
				addMacroDevice(device);
			} catch (IOException e) {
				LOG.log(Level.ERROR, "Failed to add macro device.", e);
			}
		}
	}

	@Override
	public void deviceRemoved(Device device) {
		MacroDevice macroDevice = reverseMacroDevices.get(device);
		if (macroDevice != null) {
			LOG.log(Level.INFO, String.format("Removing macro handling for %s", device.getName()));
			try {
				macroSystem.removeDevice(macroDevice);
			} catch (IOException e) {
				LOG.log(Level.ERROR, String.format("Failed to remove macro handling for %s", device.getName()), e);
			}
			reverseMacroDevices.remove(device);
			macroDevices.remove(macroDevice);
		}
		includeDevicePatterns.remove(device.getName());
		excludeDevicePatterns.remove(device.getName());
	}

	@Override
	public void recordingStateChange(RecordingSession session) {
		if (session.getRecordingState() == RecordingState.WAITING_FOR_TARGET_KEY) {
			Toast.toast(ToastType.INFO, bundle.getString("recording.waitingForTarget.title"),
					bundle.getString("recording.waitingForTarget"));
		} else if (session.getRecordingState() == RecordingState.WAITING_FOR_EVENTS) {
			Toast.toast(ToastType.INFO, bundle.getString("recording.waitingForEvents.title"),
					bundle.getString("recording.waitingForEvents"));
		}

	}

	@Override
	public void eventRecorded() {

	}
}