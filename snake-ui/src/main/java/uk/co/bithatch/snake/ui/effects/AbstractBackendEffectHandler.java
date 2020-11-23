package uk.co.bithatch.snake.ui.effects;

import java.net.URL;
import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.ui.AbstractBackendEffectController;

public abstract class AbstractBackendEffectHandler<E extends Effect, O extends AbstractBackendEffectController<E, ?>>
		extends AbstractEffectHandler<E, O> {

	private Class<E> clazz;
	private Class<O> controllerClass;

	protected AbstractBackendEffectHandler(Class<E> clazz, Class<O> controllerClass) {
		this.clazz = clazz;
		this.controllerClass = controllerClass;
	}

	@Override
	public URL getEffectImage(int size) {
		return getContext().getConfiguration().themeProperty().getValue().getEffectImage(size, clazz);
	}

	@Override
	public Class<O> getOptionsController() {
		return controllerClass;
	}

	@Override
	public boolean isSupported(Lit component) {
		return component.getSupportedEffects().contains(clazz);

	}

	public Class<E> getBackendEffectClass() {
		return clazz;
	}

	@Override
	public String getDisplayName() {
		return bundle.getString("effect." + clazz.getSimpleName());
	}

	public void save(Lit component, E effect) {
		onSave(getEffectPreferences(component), effect);
	}

	public void load(Lit component, E effect) {
		onLoad(getEffectPreferences(component), effect);
	}

	@Override
	protected void onActivate(Lit component) {
		E effect = (E) component.createEffect(clazz);

		/* Load the configuration for this effect */
		onLoad(getEffectPreferences(component), effect);

		getContext().getScheduler().execute(() -> component.setEffect(effect));
	}

	protected void onLoad(Preferences prefs, E effect) {
	}

	protected void onSave(Preferences prefs, E effect) {
	}
}
