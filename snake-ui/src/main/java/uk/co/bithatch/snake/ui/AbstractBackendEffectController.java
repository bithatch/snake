package uk.co.bithatch.snake.ui;

import java.util.Objects;

import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.ui.effects.AbstractBackendEffectHandler;

public abstract class AbstractBackendEffectController<E extends Effect, H extends AbstractBackendEffectHandler<E, ?>>
		extends AbstractEffectController<E, H> {

	private E effect;

	public E getEffect() {
		return effect;
	}

	public final void setEffect(E effect) {
		this.effect = effect;
		onSetEffect();
	}

	protected void onSetEffect() {
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onSetEffectHandler() {
		Effect deviceEffect = getRegion().getEffect();
		Class<?> deviceEffectClass = deviceEffect == null ? null : deviceEffect.getClass();
		Class<? extends Effect> requiredEffectClass = getEffectHandler().getBackendEffectClass();
		if (Objects.equals(requiredEffectClass, deviceEffectClass)) {
			setEffect((E) deviceEffect);
		} else {
			E newEffect = (E) getRegion().createEffect(requiredEffectClass);
			getEffectHandler().load(getRegion(), newEffect);
			setEffect(newEffect);
		}
	}

}
