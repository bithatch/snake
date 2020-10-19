package uk.co.bithatch.snake.ui;

import uk.co.bithatch.snake.lib.effects.Starlight;

public class StarlightEffectOptions implements EffectOptions<Starlight> {

	@Override
	public Class<? extends AbstractEffectController<Starlight>> getOptionsController() {
		return StarlightOptions.class;
	}

	@Override
	public Class<Starlight> getEffectClass() {
		return Starlight.class;
	}

}
