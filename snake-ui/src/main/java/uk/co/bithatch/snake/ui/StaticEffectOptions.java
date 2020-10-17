package uk.co.bithatch.snake.ui;

import uk.co.bithatch.snake.lib.effects.Static;

public class StaticEffectOptions implements EffectOptions<Static> {

	@Override
	public Class<? extends AbstractEffectController<Static>> getOptionsController() {
		return StaticOptions.class;
	}

	@Override
	public Class<Static> getEffectClass() {
		return Static.class;
	}

}
