package uk.co.bithatch.snake.ui;

import uk.co.bithatch.snake.lib.effects.Breath;

public class BreathEffectOptions implements EffectOptions<Breath> {

	@Override
	public Class<? extends AbstractEffectController<Breath>> getOptionsController() {
		return BreathOptions.class;
	}

	@Override
	public Class<Breath> getEffectClass() {
		return Breath.class;
	}

}
