package uk.co.bithatch.snake.ui.effects;

import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.On;
import uk.co.bithatch.snake.ui.AbstractBackendEffectController;

public class OnEffectHandler extends AbstractBackendEffectHandler<On, AbstractBackendEffectController<On, ?>> {
	public OnEffectHandler() {
		super(On.class, null);
	}

	@Override
	protected void onStore(Lit component, AbstractBackendEffectController<On, ?> controller) throws Exception {
	}
}
