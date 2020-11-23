package uk.co.bithatch.snake.ui.effects;

import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Pulsate;
import uk.co.bithatch.snake.ui.AbstractBackendEffectController;

public class PulsateEffectHandler
		extends AbstractBackendEffectHandler<Pulsate, AbstractBackendEffectController<Pulsate, ?>> {

	public PulsateEffectHandler() {
		super(Pulsate.class, null);
	}

	@Override
	protected void onStore(Lit component, AbstractBackendEffectController<Pulsate, ?> controller) throws Exception {
	}
}
