package uk.co.bithatch.snake.ui.effects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Stack;
import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.EffectHandler;
import uk.co.bithatch.snake.ui.util.Prefs;

public class EffectManager {

	public interface Listener {
		void effectAdded(Device component, EffectHandler<?, ?> effect);

		void effectRemoved(Device component, EffectHandler<?, ?> effect);
	}

	public static final String PREF_EFFECT = "effect";

	final static System.Logger LOG = System.getLogger(EffectManager.class.getName());

	static final String PREF_CUSTOM_EFFECTS = "customEffects";

	private Map<Lit, EffectHandler<?, ?>> active = Collections.synchronizedMap(new HashMap<>());
	private App context;
	private Map<Device, Map<String, EffectHandler<?, ?>>> effectHandlers = new LinkedHashMap<>();
	private List<Listener> listeners = new ArrayList<>();
	private Map<Device, Stack<EffectAcquisition>> acquisitions = new HashMap<>();

	public EffectManager(App context) {
		this.context = context;
	}

	public EffectAcquisition getRootAcquisition(Device device) {
		synchronized (acquisitions) {
			return acquisitions.get(device).firstElement();
		}
	}

	public EffectAcquisition getHeadAcquisition(Device device) {
		synchronized (acquisitions) {
			return acquisitions.get(device).peek();
		}
	}

	public EffectAcquisition acquire(Device device) {
		Stack<EffectAcquisition> stack = getStack(device);
		EffectAcquisition iface = new EffectAcquisition() {

			private Map<Lit, EffectHandler<?, ?>> effects = new HashMap<>();

			private List<EffectChangeListener> listeners = new ArrayList<>();

			@Override
			public void close() throws Exception {
				synchronized (acquisitions) {
					stack.remove(this);

					/* If no acquisitions left, turn off effects */
					if (acquisitions.isEmpty()) {
						activate(device, EffectManager.this.getEffect(device, OffEffectHandler.class));
					} else {
						/* Return to the previous acquisitions effect */
						Stack<EffectAcquisition> deviceStack = acquisitions.get(device);
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

			public void addListener(EffectChangeListener listener) {
				this.listeners.add(listener);
			}

			public void removeListener(EffectChangeListener listener) {
				this.listeners.remove(listener);
			}

			@Override
			public EffectHandler<?, ?> getEffect(Lit lit) {
				if (effects.containsKey(lit)) {
					return effects.get(lit);
				} else {
					String effectName = getPreferences(lit).get(EffectManager.PREF_EFFECT, "");
					return EffectManager.this.getEffect(Lit.getDevice(lit), effectName);
				}
			}

			@Override
			public void activate(Lit lit, EffectHandler<?, ?> effect) {
				effects.put(lit, effect);
				if (lit instanceof Device) {
					for (Region r : ((Device) lit).getRegions())
						effects.put(r, effect);
				}
				EffectManager.this.activate(lit, effect);
				for (int i = listeners.size() - 1; i >= 0; i--)
					listeners.get(i).effectChanged(lit, effect);
			}

			@Override
			public void update(Lit lit) {
				/*
				 * Only update if this is actually the head aquisition. We will update the
				 * current effect when all acquisitions are released and this becomes the head.
				 */
				synchronized (acquisitions) {
					if (this == stack.peek()) {
						EffectHandler<?, ?> litEffect = effects.get(lit);
						if (litEffect != null)
							litEffect.update(lit);
					}
				}
			}

			@Override
			public Set<Lit> getLitAreas() {
				return effects.keySet();
			}

			@Override
			public Device getDevice() {
				return device;
			}
		};
		stack.add(iface);
		return iface;
	}

	protected Stack<EffectAcquisition> getStack(Device device) {
		Stack<EffectAcquisition> stack = acquisitions.get(device);
		if (stack == null) {
			stack = new Stack<>();
			acquisitions.put(device, stack);
		}
		return stack;
	}

	private void activate(Lit component, EffectHandler<?, ?> effect) {
		synchronized (active) {
			deactivateComponent(component);
			if (effect != null) {
				effect.activate(component);
				active.put(component, effect);
			}
		}
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

	@SuppressWarnings("unchecked")
	private <H extends EffectHandler<?, ?>> H getEffect(Lit component, Class<H> clazz) {
		for (EffectHandler<?, ?> eh : getEffects(component)) {
			if (eh.getClass().equals(clazz))
				return (H) eh;
		}
		return null;
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

	public boolean isSupported(Lit component, Class<? extends EffectHandler<?, ?>> clazz) {
		return getEffect(component, clazz) != null || (clazz.equals(CustomEffectHandler.class)
				&& Lit.getDevice(component).getCapabilities().contains(Capability.MATRIX));
	}

	public void remove(EffectHandler<?, ?> handler) {

		Map<String, EffectHandler<?, ?>> deviceEffectHandlers = getDeviceEffectHandlers(handler.getDevice());
		if (deviceEffectHandlers.containsKey(handler.getName())) {
			synchronized (active) {
				/* Deactivate the effect on any device or region that is using it */
				for (Lit r : new ArrayList<>(active.keySet())) {
					if (active.get(r).equals(handler)) {
						deactivateComponent(r);
					}
				}
			}

			deviceEffectHandlers.remove(handler.getName());
			handler.removed(handler.getDevice());
			if (handler.equals(active.get(handler.getDevice()))) {
				/* We are removing the currently selected device effect */
				Set<EffectHandler<?, ?>> effectsNow = getEffects(handler.getDevice());
				if (!effectsNow.isEmpty())
					activate(handler.getDevice(), effectsNow.iterator().next());
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

	protected void deactivateComponent(Lit component) {
		EffectHandler<?, ?> deviceEffect = active.get(component);
		if (deviceEffect != null) {
			deviceEffect.deactivate(component);
			active.remove(component);
		}
	}

}
