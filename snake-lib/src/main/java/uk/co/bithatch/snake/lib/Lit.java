package uk.co.bithatch.snake.lib;

import uk.co.bithatch.snake.lib.effects.Effect;

public interface Lit extends Item {

	Effect getEffect();

	void setEffect(Effect effect);

	default boolean isSupported(Effect effect) {
		return getSupportedEffects().contains(effect.getClass());
	}

	Effect createEffect(Class<? extends Effect> clazz);
}
