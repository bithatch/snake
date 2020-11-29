package uk.co.bithatch.snake.ui.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Stack;
import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.EffectHandler;
import uk.co.bithatch.snake.ui.util.Prefs;

public class EffectManager {

	private final class EffectAcquisitionImpl implements EffectAcquisition {
		private final Device device;
		private final Stack<EffectAcquisitionImpl> stack;
		private Map<Region, EffectHandler<?, ?>> effects = new HashMap<>();
		private List<EffectChangeListener> listeners = new ArrayList<>();
		private Map<Region, EffectHandler<?, ?>> active = Collections.synchronizedMap(new HashMap<>());
		private Map<Region, Object> activations = Collections.synchronizedMap(new HashMap<>());

		private EffectAcquisitionImpl(Device device, Stack<EffectAcquisitionImpl> stack) {
			this.device = device;
			this.stack = stack;
		}

		@SuppressWarnings("unchecked")
		public <E extends Effect> E getEffectInstance(Lit component) {
			return (E) activations.get(component);
		}

		@Override
		public void activate(Lit lit, EffectHandler<?, ?> effect) {
			if (lit instanceof Device) {
				for (Region r : ((Device) lit).getRegions())
					effects.put(r, effect);
			} else
				effects.put((Region) lit, effect);
			doActivate(lit, effect);
			for (int i = listeners.size() - 1; i >= 0; i--)
				listeners.get(i).effectChanged(lit, effect);
		}

		public void addListener(EffectChangeListener listener) {
			this.listeners.add(listener);
		}

		@Override
		public void close() throws Exception {
			synchronized (acquisitions) {
				stack.remove(this);

				/* If no acquisitions left, turn off effects */
				if (acquisitions.isEmpty()) {
					activate(device, EffectManager.this.getEffect(device, OffEffectHandler.class));
				} else {
					/* Return to the previous acquisitions effect */
					Stack<EffectAcquisitionImpl> deviceStack = acquisitions.get(device);
					if (deviceStack != null) {
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
			if (lit instanceof Device) {
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
			} else {
				if (effects.containsKey(lit)) {
					return effects.get(lit);
				} else {
					String effectName = getPreferences(lit).get(EffectManager.PREF_EFFECT, "");
					return EffectManager.this.getEffect(Lit.getDevice(lit), effectName);
				}
			}
		}

		@Override
		public Set<Region> getLitAreas() {
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

		protected void deactivateComponent(Lit component) {
			EffectHandler<?, ?> deviceEffect = active.get(component);
			if (deviceEffect != null) {
				deviceEffect.deactivate(component);
				active.remove(component);
				activations.remove(component);
			}
		}

		private void doActivate(Lit component, EffectHandler<?, ?> effect) {
			synchronized (active) {
				deactivateComponent(component);
				if (effect != null) {
					effect.activate(component);
					if (component instanceof Device) {
						for (Region r : ((Device) component).getRegions()) {
							activateRegion(r, effect);
						}
					} else {
						activateRegion((Region) component, effect);
					}
				}
			}
		}

		private void activateRegion(Region component, EffectHandler<?, ?> effect) {
			if (component.getSupportedEffects().contains(effect.getBackendEffectClass())) {
				activations.put(component, effect.activate(component));
				active.put((Region) component, effect);
			}
		}
	}

	public interface Listener {
		void effectAdded(Device component, EffectHandler<?, ?> effect);

		void effectRemoved(Device component, EffectHandler<?, ?> effect);
	}

	public static final String PREF_EFFECT = "effect";

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
		EffectAcquisitionImpl iface = new EffectAcquisitionImpl(device, stack);
		stack.add(iface);
		return iface;
	}

	public void add(Device device, EffectHandler<?, ?> handler) {
		if (device == null)
			throw new IllegalArgumentException("Must provide device.");

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
			return acquisitions.get(device).firstElement();
		}
	}

	public boolean isSupported(Lit component, Class<? extends EffectHandler<?, ?>> clazz) {
		return findEffect(component, clazz) != null || (clazz.equals(CustomEffectHandler.class)
				&& Lit.getDevice(component).getCapabilities().contains(Capability.MATRIX));
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
								acq.deactivateComponent(r);
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

}
