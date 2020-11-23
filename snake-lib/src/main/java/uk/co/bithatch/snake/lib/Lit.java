package uk.co.bithatch.snake.lib;

import uk.co.bithatch.snake.lib.effects.Effect;

public interface Lit extends Item {

	<E extends Effect> E createEffect(Class<E> clazz);

	Effect getEffect();

	default boolean isSupported(Effect effect) {
		return getSupportedEffects().contains(effect.getClass());
	}

	void setEffect(Effect effect);

	void updateEffect(Effect effect);

	static Device getDevice(Lit component) {
		return component == null ? null
				: (component instanceof Device ? ((Device) component) : ((Region) component).getDevice());
	}
}
