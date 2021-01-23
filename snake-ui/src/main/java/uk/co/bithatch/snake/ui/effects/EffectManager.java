package uk.co.bithatch.snake.ui.effects;

import java.io.Closeable;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Stack;
import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Backend.BackendListener;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.lib.effects.Off;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.EffectHandler;
import uk.co.bithatch.snake.ui.util.Prefs;

public class EffectManager implements BackendListener, Closeable {

	private final class EffectAcquisitionImpl implements EffectAcquisition {
		private final Device device;
		private final Stack<EffectAcquisitionImpl> stack;
		private Map<Lit, EffectHandler<?, ?>> effects = new HashMap<>();
		private List<EffectChangeListener> listeners = new ArrayList<>();
		private Map<Lit, EffectHandler<?, ?>> active = Collections.synchronizedMap(new HashMap<>());
		private Map<Lit, Object> activations = Collections.synchronizedMap(new HashMap<>());

		private EffectAcquisitionImpl(Device device, Stack<EffectAcquisitionImpl> stack) {
			this.device = device;
			this.stack = stack;
		}

		@SuppressWarnings("unchecked")
		public <E extends Effect> E getEffectInstance(Lit component) {
			return (E) activations.get(component);
		}

		@Override
		public void deactivate(Lit lit) {
			/* This current effect */
			EffectHandler<?, ?> effect = effects.get(lit);
			if (effect != null) {
				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG,
							String.format("De-activating effect %s on %s", effect.getDisplayName(), lit.toString()));
				effect.deactivate(lit);
			}
		}

		@Override
		public void remove(Lit lit) {
			/* This current effect */
			EffectHandler<?, ?> effect = effects.remove(lit);

			if (effect != null) {

				if (LOG.isLoggable(Level.INFO))
					LOG.log(Level.INFO,
							String.format("Removing effect %s on %s", effect.getDisplayName(), lit.toString()));

				active.remove(lit);
				activations.remove(lit);
				effect.deactivate(lit);

				for (int i = listeners.size() - 1; i >= 0; i--)
					listeners.get(i).effectChanged(lit, effect);
			}

		}

		@Override
		public void activate(Lit lit, EffectHandler<?, ?> effect) {
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG,
						String.format("Activating effect %s on %s", effect.getDisplayName(), lit.toString()));

			/* Activate the new effect */
			if (lit.getSupportedEffects().contains(effect.getBackendEffectClass())) {
				if (effect.isRegions()) {

					if (lit instanceof Device) {
						deactivateDevicesRegions((Device) lit);
						for (Region r : ((Device) lit).getRegions())
							effects.put(r, effect);
					} else {
						deactivateLitsDevice(lit);
						effects.put(lit, effect);
					}
				} else {
					/*
					 * Effect doesn't support regions, so first of all it must only be activated as
					 * a whole Device
					 */

					if (!(lit instanceof Device)) {
						throw new IllegalArgumentException("Cannot set individual regions for this effect.");
					}

					deactivateDevicesRegions((Device) lit);

					effects.put((Device) lit, effect);
				}
				activations.put(lit, effect.activate(lit));
				active.put(lit, effect);
			} else
				throw new UnsupportedOperationException(String.format("Effect %s is not supported on %s", effect, lit));

			for (int i = listeners.size() - 1; i >= 0; i--)
				listeners.get(i).effectChanged(lit, effect);

			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("Now %d effects, %d activations, and %d active in acq. %s",
						effects.size(), activations.size(), active.size(), hashCode()));
		}

		public void addListener(EffectChangeListener listener) {
			this.listeners.add(listener);
		}

		@Override
		public void close() throws Exception {
			synchronized (acquisitions) {
				stack.remove(this);

				/*
				 * Deactivate all the effects this acquisition used. Some effects may need to
				 * turn off timers etc
				 *
				 */
				for (Lit lit : new ArrayList<>(getLitAreas())) {
					deactivate(lit);
				}

				/* If no acquisitions left, turn off effects */
				if (acquisitions.isEmpty()) {
					activate(device, EffectManager.this.getEffect(device, OffEffectHandler.class));
				} else {
					/* Return to the previous acquisitions effect */
					Stack<EffectAcquisitionImpl> deviceStack = acquisitions.get(device);
					if (deviceStack != null && !deviceStack.isEmpty()) {
						EffectAcquisition iface = deviceStack.peek();
						for (Lit subLit : iface.getLitAreas()) {
							EffectHandler<?, ?> litEffect = iface.getEffect(subLit);
							if (litEffect != null)
								activate(subLit, litEffect);
						}
					}
				}
			}
		}

		@Override
		public Device getDevice() {
			return device;
		}

		@SuppressWarnings("resource")
		@Override
		public EffectHandler<?, ?> getEffect(Lit lit) {
			if (effects.containsKey(lit)) {
				return effects.get(lit);
			} else {
				String effectName = getPreferences(lit).get(EffectManager.PREF_EFFECT, "");
				if (lit instanceof Device) {
					if (effectName.equals("")) {
						EffectHandler<?, ?> leffect = null;
						for (Region r : Lit.getDevice(lit).getRegions()) {
							EffectHandler<?, ?> reffect = getEffect(r);
							if (leffect != null && !Objects.equals(reffect, leffect)) {
								/* Different effects on different regions */
								return null;
							}
							leffect = reffect;
						}

						/* Same effect on same regions */
						return leffect;
					}
				}
				return EffectManager.this.getEffect(Lit.getDevice(lit), effectName);
			}
		}

		@Override
		public Set<Lit> getLitAreas() {
			return effects.keySet();
		}

		public void removeListener(EffectChangeListener listener) {
			this.listeners.remove(listener);
		}

		@Override
		public void update(Lit lit) {
			/*
			 * Only update if this is actually the head aquisition. We will update the
			 * current effect when all acquisitions are released and this becomes the head.
			 */
			synchronized (acquisitions) {
				if (this == stack.peek()) {
					if (lit instanceof Device) {
						for (Region r : Lit.getDevice(lit).getRegions()) {
							EffectHandler<?, ?> litEffect = effects.get(r);
							if (litEffect != null)
								litEffect.update(r);
						}
					} else {
						EffectHandler<?, ?> litEffect = effects.get(lit);
						if (litEffect != null)
							litEffect.update(lit);
					}
				}
			}
		}

		@Override
		public <E extends EffectHandler<?, ?>> E activate(Lit region, Class<E> class1) {
			E fx = findEffect(region, class1);
			if (fx != null)
				activate(region, fx);
			return fx;
		}

		protected void deactivateDevicesRegions(Device lit) {
			/* If there are region region effects, deactivate and remove those first */
			for (Region r : lit.getRegions()) {
				EffectHandler<?, ?> regionEffect = effects.remove(r);
				if (regionEffect != null) {
					if (LOG.isLoggable(Level.DEBUG))
						LOG.log(Level.DEBUG, String.format("Deactivating devices region effect %s on %s",
								regionEffect.getDisplayName(), lit.toString()));
					regionEffect.deactivate(r);
					active.remove(r);
					activations.remove(r);
				}
			}
			deactivateLitsDevice(lit);
		}

		protected void deactivateLitsDevice(Lit region) {
			/*
			 * Effect supports regions, so remove and deactivate any device effect and if
			 * activating a device, replace with individual regions.
			 */
			Device litDevice = Lit.getDevice(region);
			EffectHandler<?, ?> deviceEffect = effects.remove(litDevice);
			if (deviceEffect != null) {
				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG, String.format("Deactivating regions device effect %s on %s",
							deviceEffect.getDisplayName(), region.toString()));

				deviceEffect.deactivate(litDevice);
				active.remove(litDevice);
				activations.remove(litDevice);
			}
		}
	}

	public interface Listener {
		void effectAdded(Device component, EffectHandler<?, ?> effect);

		void effectRemoved(Device component, EffectHandler<?, ?> effect);
	}

	public static final String PREF_EFFECT = "effect";
	public static final String PREF_SYNC = "sync";

	final static System.Logger LOG = System.getLogger(EffectManager.class.getName());

	static final String PREF_CUSTOM_EFFECTS = "customEffects";

	private Map<Device, Stack<EffectAcquisitionImpl>> acquisitions = new HashMap<>();
	private App context;
	private Map<Device, Map<String, EffectHandler<?, ?>>> effectHandlers = Collections
			.synchronizedMap(new LinkedHashMap<>());
	private List<Listener> listeners = new ArrayList<>();

	public EffectManager(App context) {
		this.context = context;
	}

	public EffectAcquisition acquire(Device device) {
		Stack<EffectAcquisitionImpl> stack = getStack(device);
		if (!stack.isEmpty()) {
			stack.peek().deactivate(device);
		}
		EffectAcquisitionImpl iface = new EffectAcquisitionImpl(device, stack);
		stack.add(iface);
		return iface;
	}

	public void add(Device device, EffectHandler<?, ?> handler) {
		if (device == null)
			throw new IllegalArgumentException("Must provide device.");

		if (LOG.isLoggable(Level.DEBUG))
			LOG.log(Level.DEBUG, String.format("Adding effect handler %s to device %s", device.getName(),
					handler.getClass().getName()));
		Map<String, EffectHandler<?, ?>> deviceEffectHandlers = getDeviceEffectHandlers(device);
		checkForName(handler, deviceEffectHandlers);

		if (handler.getName() == null || handler.getName().equals(""))
			throw new IllegalArgumentException("No name.");
		if (handler.getContext() == null)
			handler.open(context, device);
		deviceEffectHandlers.put(handler.getName(), handler);
		handler.added(device);
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).effectAdded(device, handler);
	}

	public void open() {
		context.getBackend().addListener(this);
		context.getBackend().setSync(context.getPreferences().getBoolean(PREF_SYNC, false));
	}

	public void addListener(Listener listener) {
		this.listeners.add(listener);
	}

	public Map<String, EffectHandler<?, ?>> getDeviceEffectHandlers(Device device) {
		Map<String, EffectHandler<?, ?>> deviceEffectHandlers = effectHandlers.get(device);
		if (deviceEffectHandlers == null) {
			deviceEffectHandlers = new LinkedHashMap<>();
			effectHandlers.put(device, deviceEffectHandlers);
			for (EffectHandler<?, ?> handler : ServiceLoader.load(EffectHandler.class)) {
				addEffect(device, handler);
			}
			Preferences devPrefs = getPreferences(device);
			for (String custom : Prefs.getStringSet(devPrefs, PREF_CUSTOM_EFFECTS)) {
				CustomEffectHandler effect = new CustomEffectHandler();
				effect.setName(custom);
				effect.load(devPrefs.node(custom));
				addEffect(device, effect);
			}
		}
		return deviceEffectHandlers;
	}

	public Preferences getDevicePreferences(Device device) {
		return context.getPreferences(device).node("effects");
	}

	@SuppressWarnings("unchecked")
	public <E extends AbstractBackendEffectHandler<?, ?>> E getEffect(Device device, Class<E> effect) {
		return (E) getEffect(device, effect.getSimpleName());
	}

	@SuppressWarnings("unchecked")
	public <H extends EffectHandler<?, ?>> H getEffect(Device device, String name) {
		return (H) getDeviceEffectHandlers(device).get(name);
	}

	public Set<EffectHandler<?, ?>> getEffects(Lit component) {
		Set<EffectHandler<?, ?>> filteredHandlers = new LinkedHashSet<>();
		Map<String, EffectHandler<?, ?>> deviceEffectHandlers = getDeviceEffectHandlers(Lit.getDevice(component));
		for (EffectHandler<?, ?> h : deviceEffectHandlers.values()) {
			if (h.isSupported(component)) {
				filteredHandlers.add(h);
			}
		}
		return filteredHandlers;
	}

	public EffectAcquisition getHeadAcquisition(Device device) {
		synchronized (acquisitions) {
			return acquisitions.get(device).peek();
		}
	}

	public Preferences getPreferences(Lit component) {
		if (component instanceof Device)
			return getDevicePreferences((Device) component).node("device");
		else if (component instanceof Region)
			return getDevicePreferences(((Region) component).getDevice()).node("region")
					.node(((Region) component).getName().name());
		else
			throw new IllegalArgumentException(
					String.format("Unsuported %s type %s", Lit.class.getName(), component.getClass().getName()));
	}

	public EffectAcquisition getRootAcquisition(Device device) {
		synchronized (acquisitions) {
			Stack<EffectAcquisitionImpl> stack = getStack(device);
			return stack.isEmpty() ? null : stack.firstElement();
		}
	}

	public boolean isSupported(Lit component, Class<? extends EffectHandler<?, ?>> clazz) {
		return findEffect(component, clazz) != null || isMatrixEffect(component, clazz);
	}

	protected boolean isMatrixEffect(Lit component, Class<? extends EffectHandler<?, ?>> clazz) {
		/* TODO make this better */
		return (clazz.equals(CustomEffectHandler.class) || clazz.equals(AudioEffectHandler.class))
				&& Lit.getDevice(component).getCapabilities().contains(Capability.MATRIX);
	}

	public void remove(EffectHandler<?, ?> handler) {

		Map<String, EffectHandler<?, ?>> deviceEffectHandlers = getDeviceEffectHandlers(handler.getDevice());
		if (deviceEffectHandlers.containsKey(handler.getName())) {
			/* Deactivate the effect on any device or region that is using it */
			synchronized (acquisitions) {
				for (Map.Entry<Device, Stack<EffectAcquisitionImpl>> en : acquisitions.entrySet()) {
					for (EffectAcquisitionImpl acq : en.getValue()) {
						for (Lit r : new ArrayList<>(acq.active.keySet())) {
							if (acq.active.get(r).equals(handler)) {
								acq.deactivate(r);
							}
						}
					}
				}
			}

			deviceEffectHandlers.remove(handler.getName());
			handler.removed(handler.getDevice());

			/*
			 * Set the first available effect back on every region on every device in the
			 * stack
			 */
			synchronized (acquisitions) {
				for (Map.Entry<Device, Stack<EffectAcquisitionImpl>> en : acquisitions.entrySet()) {
					for (EffectAcquisitionImpl acq : en.getValue()) {
						Set<EffectHandler<?, ?>> effectsNow = getEffects(handler.getDevice());
						if (!effectsNow.isEmpty())
							acq.activate(handler.getDevice(), effectsNow.iterator().next());
					}
				}
			}

			for (int i = listeners.size() - 1; i >= 0; i--)
				listeners.get(i).effectRemoved(handler.getDevice(), handler);
		}
	}

	public void removeListener(Listener listener) {
		this.listeners.remove(listener);
	}

	protected void addEffect(Device device, EffectHandler<?, ?> handler) {
		Map<String, EffectHandler<?, ?>> deviceEffectHandlers = getDeviceEffectHandlers(device);
		if (deviceEffectHandlers.containsKey(handler.getName())) {
			throw new IllegalStateException(String.format(
					"Attempt to register more than one effect named %s. Please rename the %s implementation, only the class name itself is used and must be unique.",
					handler.getName(), EffectHandler.class.getName()));
		}
		deviceEffectHandlers.put(handler.getName(), handler);
		if (LOG.isLoggable(Level.DEBUG))
			LOG.log(Level.DEBUG,
					String.format("Adding effect %s, serviced by %s", handler.getName(), handler.getClass().getName()));
		handler.open(context, device);
	}

	protected void checkForName(EffectHandler<?, ?> handler, Map<String, EffectHandler<?, ?>> deviceEffectHandlers) {
		if (deviceEffectHandlers.containsKey(handler.getName())) {
			throw new IllegalStateException(String.format(
					"Attempt to register more than one effect named %s. Please rename the %s implementation, only the class name itself is used and must be unique.",
					handler.getName(), EffectHandler.class.getName()));
		}
	}

	protected Stack<EffectAcquisitionImpl> getStack(Device device) {
		Stack<EffectAcquisitionImpl> stack = acquisitions.get(device);
		if (stack == null) {
			stack = new Stack<>();
			acquisitions.put(device, stack);
		}
		return stack;
	}

	@SuppressWarnings("unchecked")
	private <H extends EffectHandler<?, ?>> H findEffect(Lit component, Class<H> clazz) {
		for (EffectHandler<?, ?> eh : getEffects(component)) {
			if (eh.getClass().equals(clazz))
				return (H) eh;
		}
		return null;
	}

	public boolean isSync() {
		return context.getBackend().isSync();
	}

	public void setSync(boolean sync) {
		context.getBackend().setSync(sync);
		context.getPreferences().putBoolean("sync", sync);
	}

	@Override
	public void deviceAdded(Device device) {
		addDevice(device);
	}

	@Override
	public void deviceRemoved(Device device) {
		removeDevice(device);
	}

	protected void removeDevice(Device device) {
		synchronized (acquisitions) {
			Stack<EffectAcquisitionImpl> acq = acquisitions.get(device);
			while (acq != null && !acq.isEmpty()) {
				try {
					EffectAcquisitionImpl pop = acq.pop();
					pop.close();
				} catch (Exception e) {
					LOG.log(Level.ERROR, "Failed to close effect acquisition.", e);
				}
			}
			acquisitions.remove(device);
		}
	}

	public void addDevice(Device dev) {
		synchronized (acquisitions) {
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("Adding device %s to effect manager.", dev.getName()));

			/* Acquire an effects controller for this device */
			EffectAcquisition acq = acquire(dev);

			EffectHandler<?, ?> deviceEffect = acq.getEffect(dev);
			boolean activated = false;
			if (deviceEffect != null && !deviceEffect.isRegions()) {
				/* Always activates at device level */
				acq.activate(dev, deviceEffect);
				activated = true;
			} else {
				/* No effect configured for device as a whole, check the regions */
				for (Region r : dev.getRegions()) {
					EffectHandler<?, ?> regionEffect = acq.getEffect(r);
					if (regionEffect != null && regionEffect.isRegions()) {
						try {
							acq.activate(r, regionEffect);
							activated = true;
						} catch (UnsupportedOperationException uoe) {
							if (LOG.isLoggable(Level.DEBUG))
								LOG.log(Level.WARNING, "Failed to activate effect.", uoe);
						}
					}
				}
			}

			/* Now try at device level */
			if (!activated && deviceEffect != null) {
				acq.activate(dev, deviceEffect);
				activated = true;
			}

			if (!activated) {
				/* Get the first effect and activate on whole device */
				Set<EffectHandler<?, ?>> effects = getEffects(dev);
				Iterator<EffectHandler<?, ?>> it = effects.iterator();
				while (it.hasNext()) {
					EffectHandler<?, ?> fx = it.next();
					if (dev.getSupportedEffects().contains(fx.getBackendEffectClass())) {
						acq.activate(dev, fx);
						break;
					}
				}
			}
		}

	}

	@Override
	public void close() throws IOException {
		synchronized (acquisitions) {
			while (!acquisitions.isEmpty()) {
				Device device = acquisitions.keySet().iterator().next();
				if (context.getConfiguration().isTurnOffOnExit()) {
					device.setEffect(new Off());
				}
				removeDevice(device);
			}
		}

	}

}
