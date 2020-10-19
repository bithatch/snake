package uk.co.bithatch.snake.ui;

import uk.co.bithatch.snake.lib.effects.Ripple;

public class RippleEffectOptions implements EffectOptions<Ripple> {

	@Override
	public Class<? extends AbstractEffectController<Ripple>> getOptionsController() {
		return RippleOptions.class;
	}

	@Override
	public Class<Ripple> getEffectClass() {
		return Ripple.class;
	}

}
