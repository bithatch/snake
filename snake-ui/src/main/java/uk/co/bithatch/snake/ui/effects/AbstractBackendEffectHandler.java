package uk.co.bithatch.snake.ui.effects;

import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.ui.AbstractBackendEffectController;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;

public abstract class AbstractBackendEffectHandler<E extends Effect, O extends AbstractBackendEffectController<E, ?>>
		extends AbstractPersistentEffectHandler<E, O> {

	protected AbstractBackendEffectHandler(Class<E> clazz, Class<O> controllerClass) {
		super(clazz, controllerClass);
	}

	public Class<E> getBackendEffectClass() {
		return getEffectClass();
	}

	@Override
	protected final E onActivate(Lit component) {
		E effect = (E) component.createEffect(getEffectClass());

		/* Load the configuration for this effect */
		onLoad(getEffectPreferences(component), effect);

		getContext().getSchedulerManager().get(Queue.DEVICE_IO).execute(() -> component.setEffect(effect));

		return effect;
	}

}
