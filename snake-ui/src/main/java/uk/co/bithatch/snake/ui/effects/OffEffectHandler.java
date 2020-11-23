package uk.co.bithatch.snake.ui.effects;

import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Off;
import uk.co.bithatch.snake.ui.AbstractBackendEffectController;

public class OffEffectHandler extends AbstractBackendEffectHandler<Off, AbstractBackendEffectController<Off, ?>> {

	public OffEffectHandler() {
		super(Off.class, null);
	}

	@Override
	protected void onStore(Lit component, AbstractBackendEffectController<Off, ?> controller) throws Exception {
	}
}
