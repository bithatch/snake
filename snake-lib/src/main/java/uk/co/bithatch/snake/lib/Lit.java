package uk.co.bithatch.snake.lib;

import uk.co.bithatch.snake.lib.effects.Effect;

public interface Lit extends Item {

	Effect createEffect(Class<? extends Effect> clazz);

	Effect getEffect();

	default boolean isSupported(Effect effect) {
		return getSupportedEffects().contains(effect.getClass());
	}

	void setEffect(Effect effect);
}
