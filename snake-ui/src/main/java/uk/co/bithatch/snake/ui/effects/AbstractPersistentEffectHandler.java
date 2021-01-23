package uk.co.bithatch.snake.ui.effects;

import java.net.URL;
import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.ui.AbstractEffectController;

public abstract class AbstractPersistentEffectHandler<E, O extends AbstractEffectController<E, ?>>
		extends AbstractEffectHandler<E, O> {

	private Class<E> clazz;
	private Class<O> controllerClass;

	protected AbstractPersistentEffectHandler(Class<E> clazz, Class<O> controllerClass) {
		this.clazz = clazz;
		this.controllerClass = controllerClass;
	}

	@Override
	public URL getEffectImage(int size) {
		return getContext().getConfiguration().getTheme().getEffectImage(size, clazz);
	}
	
	protected Class<E> getEffectClass() {
		return clazz;
	}

	@Override
	public Class<O> getOptionsController() {
		return controllerClass;
	}

	@Override
	public boolean isSupported(Lit component) {
		return component.getSupportedEffects().contains(clazz);

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

	protected void onLoad(Preferences prefs, E effect) {
	}

	protected void onSave(Preferences prefs, E effect) {
	}
}
