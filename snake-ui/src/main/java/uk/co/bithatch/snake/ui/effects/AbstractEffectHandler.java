package uk.co.bithatch.snake.ui.effects;

import java.lang.System.Logger.Level;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.ui.AbstractEffectController;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.EffectHandler;

public abstract class AbstractEffectHandler<E, C extends AbstractEffectController<E, ?>>
		implements EffectHandler<E, C> {
	final static System.Logger LOG = System.getLogger(AbstractEffectHandler.class.getName());

	protected final static ResourceBundle bundle = ResourceBundle.getBundle(EffectHandler.class.getName());

	private App context;
	private Device device;

	@Override
	public final void open(App context, Device device) {
		this.context = context;
		this.device = device;
		onSetContext();
	}

	public final Device getDevice() {
		return device;
	}

	@Override
	public App getContext() {
		return context;
	}

	@Override
	public final E activate(Lit component) {
		select(component);
		if (component instanceof Device && isRegions()) {
			/* If device, select the same effect on all of the regions as well */
			for (Region r : ((Device) component).getRegions())
				select(r);
		}
		return onActivate(component);
	}

	@Override
	public final void select(Lit component) {
		/* Save this as the currently activated effect */
		getContext().getEffectManager().getPreferences(component).put(EffectManager.PREF_EFFECT, getName());

		if (!(component instanceof Device)) {
			/* Because switching to individual regions, clear out the saved device effect */
			getContext().getEffectManager().getPreferences(Lit.getDevice(component)).put(EffectManager.PREF_EFFECT, "");
		}
	}

	@Override
	public final void store(Lit component, C controller) {
		try {
			onStore(component, controller);
		} catch (Exception e) {
			LOG.log(Level.ERROR, "Failed to update effect.", e);
		}
	}

	@Override
	public void update(Lit component) {
	}

	protected abstract E onActivate(Lit component);

	protected abstract void onStore(Lit component, C controller) throws Exception;

	protected void onSetContext() {
	}

	protected Preferences getEffectPreferences(Lit component) {
		return getContext().getEffectManager().getPreferences(component).node(getName());
	}
}
