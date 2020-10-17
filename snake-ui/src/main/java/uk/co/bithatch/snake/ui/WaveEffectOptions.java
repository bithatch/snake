package uk.co.bithatch.snake.ui;

import uk.co.bithatch.snake.lib.effects.Wave;

public class WaveEffectOptions implements EffectOptions<Wave> {

	@Override
	public Class<? extends AbstractEffectController<Wave>> getOptionsController() {
		return WaveOptions.class;
	}

	@Override
	public Class<Wave> getEffectClass() {
		return Wave.class;
	}

}
