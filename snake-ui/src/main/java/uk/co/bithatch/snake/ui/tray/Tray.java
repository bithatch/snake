package uk.co.bithatch.snake.ui.tray;


import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
//import java.awt.AlphaComposite;
//import java.awt.Color;
//import java.awt.Font;
//import java.awt.Graphics2D;
//import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import javax.swing.JMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.fontawesome.FontAwesomeIkonHandler;

import dorkbox.systemTray.Checkbox;
import dorkbox.systemTray.Entry;
import dorkbox.systemTray.Menu;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;
import dorkbox.systemTray.util.SystemTrayFixes;
import javafx.application.Platform;
import uk.co.bithatch.macrolib.MacroSystem.RecordingListener;
import uk.co.bithatch.macrolib.RecordingSession;
import uk.co.bithatch.macrolib.RecordingState;
import uk.co.bithatch.snake.lib.Backend.BackendListener;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Device.Listener;
import uk.co.bithatch.snake.lib.Grouping;
import uk.co.bithatch.snake.lib.Item;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.binding.Profile;
import uk.co.bithatch.snake.lib.binding.ProfileMap;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.BatteryControl;
import uk.co.bithatch.snake.ui.Configuration;
import uk.co.bithatch.snake.ui.Configuration.TrayIcon;
import uk.co.bithatch.snake.ui.DeviceDetails;
import uk.co.bithatch.snake.ui.EffectHandler;
import uk.co.bithatch.snake.ui.Macros;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;
import uk.co.bithatch.snake.ui.effects.EffectAcquisition;
import uk.co.bithatch.snake.widgets.Direction;

public class Tray implements AutoCloseable, BackendListener, Listener, PreferenceChangeListener, RecordingListener {

	final static System.Logger LOG = System.getLogger(Tray.class.getName());
	
	final static ResourceBundle bundle = ResourceBundle.getBundle(Tray.class.getName());

	private Configuration cfg;
	private App context;
//	private Font font;
	private SystemTray systemTray;
	private List<Entry> menuEntries = new ArrayList<>();

	private Timer timer;

	private Font font;

	public Tray(App context) throws Exception {
		this.context = context;

//		font = Font.createFont(Font.TRUETYPE_FONT, App.class.getResourceAsStream("fontawesome-webfont.ttf"));

		cfg = context.getConfiguration();
		cfg.getNode().addPreferenceChangeListener(this);

		context.getMacroManager().getMacroSystem().addRecordingListener(this);
		context.getBackend().addListener(this);
		for (Device dev : context.getBackend().getDevices()) {
			dev.addListener(this);
		}
		SwingUtilities.invokeLater(() -> adjustTray());

	}

	@Override
	public void close() throws Exception {
		context.getMacroManager().getMacroSystem().removeRecordingListener(this);
		cfg.getNode().removePreferenceChangeListener(this);
		if (systemTray != null) {
			systemTray.shutdown();
			systemTray = null;
		}
	}

	Menu addDevice(Device device, Menu toMenu) throws IOException {
		var img = context.getDefaultImage(device.getType(), context.getCache().getCachedImage(device.getImage()));
		Menu menu = null;
		if (img.startsWith("http:") || img.startsWith("https:")) {
			var url = new URL(img);
			var path = url.getPath();
			var idx = path.lastIndexOf('/');
			if (idx != -1) {
				path = path.substring(idx + 1);
			}
			var tf = File.createTempFile("snake", path);
			tf.deleteOnExit();
			try (var fos = new FileOutputStream(tf)) {
				try (InputStream is = url.openStream()) {
					is.transferTo(fos);
					img = tf.getAbsolutePath();
				}
			} catch(IOException ioe) {
				try(InputStream in = SystemTrayFixes.class.getResource("error_32.png").openStream()) {
					menu = new Menu(device.getName(), in);
				}
				if(LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.WARNING, "Failed to load device image.", ioe);
				else
					LOG.log(Level.WARNING, "Failed to load device image. " + ioe.getMessage() );
			}
		} else if (img.startsWith("file:")) {
			img = img.substring(5);
		}
		if(toMenu == null) {
			if(menu == null)
				menu = new Menu(device.getName(), img);
		}
		else 
			menu = toMenu;

		/* Open */
		var openDev = new MenuItem(bundle.getString("open"), (e) -> Platform.runLater(() -> {
			try {
				context.open();
				context.openDevice(device);
			} catch (Exception ex) {
			}
		}));
		menu.add(openDev);
		menuEntries.add(openDev);

		if (device.getCapabilities().contains(Capability.MACROS)) {
			var macros = new MenuItem(bundle.getString("macros"), createAwesomeIcon(FontAwesome.PLAY_CIRCLE, 32),
					(e) -> {
						Platform.runLater(() -> {
							context.open();
							context.push(Macros.class, Direction.FROM_BOTTOM).setDevice(device);
						});
					});
			menu.add(macros);
			menuEntries.add(macros);
		}
		if (device.getCapabilities().contains(Capability.BRIGHTNESS)) {
			brightnessMenu(menu, device);
		}
		if (device.getCapabilities().contains(Capability.DPI)) {
			dpiMenu(menu, device);
		}
		if (device.getCapabilities().contains(Capability.POLL_RATE)) {
			pollRateMenu(menu, device);
		}
		if (device.getCapabilities().contains(Capability.GAME_MODE)) {
			gameMode(menu, device);
		}
		menu.add(new JSeparator());
		if (!device.getSupportedEffects().isEmpty()) {
			addEffectsMenu(device, menu, device, toMenu != null);
			menu.add(new JSeparator());
		}
		boolean sep;
		for (var r : device.getRegions()) {
			var mi = new Menu(bundle.getString("region." + r.getName().name()));
			mi.setImage(context.getConfiguration().getTheme().getRegionImage(24, r.getName()));
			menu.add(mi);
			if (toMenu != null)
				menuEntries.add(mi);
			sep = false;
			if (r.getCapabilities().contains(Capability.BRIGHTNESS_PER_REGION)) {
				brightnessMenu(mi, r);
				sep = true;
			}
			if (r.getCapabilities().contains(Capability.EFFECT_PER_REGION)) {
				if (sep) {
					mi.add(new JSeparator());
				}
				addEffectsMenu(r, mi, device, toMenu != null);
			}
		}
		return menu;
	}

	void addEffectsMenu(Lit lit, Menu menu, Device device, boolean addToRoot) {
		EffectHandler<?, ?> configurable = null;
		EffectAcquisition acq = context.getEffectManager().getRootAcquisition(Lit.getDevice(lit));
		if (acq == null)
			return;
		EffectHandler<?, ?> selected = acq.getEffect(lit);
		for (EffectHandler<?, ?> fx : context.getEffectManager().getEffects(lit)) {
			var mi = new MenuItem(fx.getDisplayName(), (e) -> {
				context.getSchedulerManager().get(Queue.DEVICE_IO).execute(() -> acq.activate(lit, fx));
			});
			mi.setImage(fx.getEffectImage(24));
			menu.add(mi);
			if (fx.hasOptions() && fx.equals(selected)) {
				configurable = fx;
			}
			if (addToRoot)
				menuEntries.add(mi);
		}
		EffectHandler<?, ?> fConfigurable = configurable;
		if (configurable != null) {
			var mi = new MenuItem(bundle.getString("effectOptions"), createAwesomeIcon(FontAwesome.GEAR, 32), (e) -> {
				Platform.runLater(() -> {
					context.open();
					var dev = context.push(DeviceDetails.class, Direction.FROM_RIGHT);
					dev.setDevice(device);
					dev.configure(lit, fConfigurable);
				});
			});
			menu.add(mi);
			if (addToRoot)
				menuEntries.add(mi);
		}
	}

	BufferedImage createAwesomeIcon(FontAwesome string, int sz) {
		return createAwesomeIcon(string, sz, 100);
	}

	BufferedImage createAwesomeIcon(FontAwesome string, int sz, int opac) {
		return createAwesomeIcon(string, sz, opac, null, 0);
	}

	BufferedImage createAwesomeIcon(FontAwesome fontAwesome, int sz, int opac, Color col, double rot) {
		var bim = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB);
		var graphics = (Graphics2D) bim.getGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		var szf = (int) ((float) sz * 0.75f);
		String str = Character.toString(fontAwesome.getCode());
		FontAwesomeIkonHandler handler = new FontAwesomeIkonHandler();
		if (font == null) {
			try {
				InputStream stream = handler.getFontResourceAsStream();
				font = Font.createFont(Font.TRUETYPE_FONT, stream);
				GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(font);
				stream.close();
				handler.setFont(font);
			} catch (FontFormatException | IOException ffe) {
				throw new IllegalStateException(ffe);
			}
		}
		var fnt = font.deriveFont((float) szf);
		graphics.setFont(fnt);
		if (opac != 100) {
			graphics.setComposite(makeComposite((float) opac / 100.0f));
		}
		var icon = cfg.getTrayIcon();
		if (col == null) {
			switch (icon) {
			case LIGHT:
				graphics.setColor(Color.WHITE);
				break;
			case DARK:
				graphics.setColor(Color.BLACK);
				break;
			default:
				// TODO
				break;
			}
		} else {
			graphics.setColor(col);
		}

		graphics.translate(sz / 2, sz / 2);
		graphics.rotate(Math.toRadians(rot));
		graphics.translate(-(graphics.getFontMetrics().stringWidth(str) / 2f),
				(graphics.getFontMetrics().getHeight() / 2f));
		graphics.drawString(str, 0, 0);
		return bim;
	}

	AlphaComposite makeComposite(float alpha) {
		int type = AlphaComposite.SRC_OVER;
		return (AlphaComposite.getInstance(type, alpha));
	}

	void adjustTray() {
		var icon = cfg.getTrayIcon();
		if (systemTray == null && icon != TrayIcon.OFF) {
			systemTray = SystemTray.get();
			if (systemTray == null) {
				throw new RuntimeException("Unable to load SystemTray!");
			}

			setImage();

			systemTray.setStatus(bundle.getString("title"));
			Menu menu = systemTray.getMenu();
			if (menu == null) {
				systemTray.setMenu(new JMenu(bundle.getString("title")));
			}

			rebuildMenu();

		} else if (systemTray != null && icon == TrayIcon.OFF) {
			systemTray.setEnabled(false);
		} else if (systemTray != null) {
			systemTray.setEnabled(true);
			setImage();
			rebuildMenu();
		}
	}

	private void rebuildMenu() {
		clearMenus();
		if (systemTray != null) {
			var menu = systemTray.getMenu();
			try {

				boolean devices = false;
				List<Device> devs = context.getBackend().getDevices();
				var backend = context.getBackend();
				var globals = false;

				/* Record */
				if (context.getMacroManager().getMacroSystem().isRecording()) {
					var recordDev = new MenuItem(bundle.getString("stopRecording"), (e) -> Platform.runLater(() -> {
						context.getMacroManager().getMacroSystem().stopRecording();
					}));
					menu.add(recordDev);
					menuEntries.add(recordDev);
					if (context.getMacroManager().getMacroSystem().getRecordingSession()
							.getRecordingState() == RecordingState.PAUSED) {
						var unpauseDev = new MenuItem(bundle.getString("unpauseRecording"),
								(e) -> Platform.runLater(() -> {
									context.getMacroManager().getMacroSystem().togglePauseRecording();
								}));
						menu.add(unpauseDev);
						menuEntries.add(unpauseDev);

					} else {
						var pauseDev = new MenuItem(bundle.getString("pauseRecording"), (e) -> Platform.runLater(() -> {
							context.getMacroManager().getMacroSystem().togglePauseRecording();
						}));
						menu.add(pauseDev);
						menuEntries.add(pauseDev);
					}
				} else {
					var recordDev = new MenuItem(bundle.getString("record"), (e) -> Platform.runLater(() -> {
						context.getMacroManager().getMacroSystem().startRecording();
					}));
					menu.add(recordDev);
					menuEntries.add(recordDev);
				}

				if (devs.size() == 1) {
					addDevice(devs.get(0), menu);
					devices = true;
				} else {
					var mi = new MenuItem(bundle.getString("open"), (e) -> context.open());
					menuEntries.add(mi);
					menu.add(mi);
					addSeparator(menu);

					if (backend.getCapabilities().contains(Capability.BRIGHTNESS)) {
						brightnessMenu(menu, backend);
						globals = true;
					}
					if (backend.getCapabilities().contains(Capability.GAME_MODE)) {
						gameMode(menu, backend);
						globals = true;
					}
					if (globals)
						addSeparator(menu);
					for (Device dev : devs) {
						var devmenu = addDevice(dev, null);
						systemTray.getMenu().add(devmenu);
						menuEntries.add(devmenu);
						devices = true;
					}
				}
				if (devices)
					addSeparator(menu);
			} catch (Exception e) {
				// TODO add error item / tooltip?
				systemTray.setTooltip("Erro!");
				e.printStackTrace();
			}

			var quit = new MenuItem(bundle.getString("quit"), (e) -> context.close(true));
			menuEntries.add(quit);
			menu.add(quit).setShortcut('q');
		}
	}

	private void addSeparator(Menu menu) {
		Separator sep = new Separator();
		menu.add(sep);
		menuEntries.add(sep);

	}

	private void clearMenus() {
		if (systemTray != null) {
			var menu = systemTray.getMenu();
			for (Entry dev : menuEntries) {
				menu.remove(dev);
			}
		}
		menuEntries.clear();
	}

	private void gameMode(Menu menu, Grouping backend) {

		Checkbox checkbox = new Checkbox();
		checkbox.setCallback((e) -> backend.setGameMode(checkbox.getChecked()));
		checkbox.setEnabled(true);
		checkbox.setChecked(backend.isGameMode());
		checkbox.setShortcut('g');
		checkbox.setText(bundle.getString("gameMode"));

		menu.add(checkbox);
		menuEntries.add(checkbox);
	}

	private void brightnessMenu(Menu menu, Item backend) {
		var bri = new Menu(bundle.getString("brightness"), createAwesomeIcon(FontAwesome.SUN_O, 32));
		menu.add(bri);
		for (String b : new String[] { "off", "10", "20", "50", "75", "100" }) {
			bri.add(new MenuItem(bundle.getString("brightness." + b),
					createAwesomeIcon(b.equals("off") ? FontAwesome.TOGGLE_OFF : FontAwesome.SUN_O, 32,
							b.equals("off") ? 25 : Integer.parseInt(b)),
					(e) -> {
						backend.setBrightness(b.equals("off") ? 0 : Short.parseShort(b));
					}));
		}
		menuEntries.add(bri);
	}

	private void dpiMenu(Menu menu, Device device) {
		var dpiMenu = new Menu(bundle.getString("dpi"), createAwesomeIcon(FontAwesome.MOUSE_POINTER, 32));
		menu.add(dpiMenu);
		short[] dpis = new short[] { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1500, 2000, 3000, 4000, 8000,
				16000, 32000 };
		for (short dpi : dpis) {
			if (dpi <= device.getMaxDPI()) {
				dpiMenu.add(new MenuItem(MessageFormat.format(bundle.getString("deviceDpi"), dpi), (e) -> {
					device.setDPI(dpi, dpi);
				}));
			}
		}
		menuEntries.add(dpiMenu);
	}

	private void pollRateMenu(Menu menu, Device device) {
		var pollRateMenu = new Menu(bundle.getString("pollRate"), createAwesomeIcon(FontAwesome.SIGNAL, 32));
		menu.add(pollRateMenu);
		int[] pollRates = new int[] { 125, 500, 1000 };
		for (int pollRate : pollRates) {
			pollRateMenu.add(new MenuItem(MessageFormat.format(bundle.getString("devicePollRate"), pollRate), (e) -> {
				device.setPollRate(pollRate);
			}));
		}
		menuEntries.add(pollRateMenu);
	}

	private void setImage() {
		var icon = cfg.getTrayIcon();
		boolean recording = context.getMacroManager().getMacroSystem().isRecording();
		if (!recording && context.getBackend().getCapabilities().contains(Capability.BATTERY) && cfg.isShowBattery()
				&& (!cfg.isWhenLow() || (cfg.isWhenLow()
						&& context.getBackend().getBattery() <= context.getBackend().getLowBatteryThreshold()))) {
			Color col = null;
			String status = BatteryControl.getBatteryStyle(context.getBackend().getLowBatteryThreshold(),
					context.getBackend().getBattery());
			if ("danger".equals(status))
				col = Color.RED;
			else if ("warning".equals(status))
				col = Color.ORANGE;
			systemTray.setImage(createAwesomeIcon(BatteryControl.getBatteryIcon(context.getBackend().getBattery()), 32,
					100, col, 270));
		} else {
			String name = "tray";
			if (recording)
				name = "tray-recording";
			switch (icon) {
			case LIGHT:
				systemTray.setImage(App.class.getResource("icons/" + name + "-light64.png"));
				break;
			case DARK:
				systemTray.setImage(App.class.getResource("icons/" + name + "-dark64.png"));
				break;
			case AUTO:
				// TODO
			case COLOR:
			default:
				systemTray.setImage(context.getConfiguration().getTheme().getResource("icons/" + name + "64.png"));
				break;
			}
		}
	}

	@Override
	public void deviceAdded(Device device) {
		device.addListener(this);
		SwingUtilities.invokeLater(() -> rebuildMenu());
	}

	@Override
	public void deviceRemoved(Device device) {
		device.removeListener(this);
		SwingUtilities.invokeLater(() -> rebuildMenu());
	}

	@Override
	public void changed(Device device, Region region) {
		SwingUtilities.invokeLater(() -> {
			if (timer == null) {
				timer = new Timer(500, (e) -> {
					rebuildMenu();
				});
				timer.setRepeats(false);
			}
			timer.restart();
		});
	}

	@Override
	public void activeMapChanged(ProfileMap map) {
	}

	@Override
	public void profileAdded(Profile profile) {
	}

	@Override
	public void profileRemoved(Profile profile) {
	}

	@Override
	public void mapAdded(ProfileMap profile) {
	}

	@Override
	public void mapChanged(ProfileMap profile) {
	}

	@Override
	public void mapRemoved(ProfileMap profile) {
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent evt) {
		if (evt.getKey().equals(Configuration.PREF_THEME) || evt.getKey().equals(Configuration.PREF_TRAY_ICON)
				|| evt.getKey().equals(Configuration.PREF_SHOW_BATTERY)
				|| evt.getKey().equals(Configuration.PREF_WHEN_LOW)) {
			SwingUtilities.invokeLater(() -> adjustTray());
		}
	}

	@Override
	public void recordingStateChange(RecordingSession session) {
		SwingUtilities.invokeLater(() -> adjustTray());
	}

	@Override
	public void eventRecorded() {
	}
}
