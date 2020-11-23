package uk.co.bithatch.snake.ui.effects;

import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Spectrum;
import uk.co.bithatch.snake.ui.AbstractBackendEffectController;

public class SpectrumEffectHandler
		extends AbstractBackendEffectHandler<Spectrum, AbstractBackendEffectController<Spectrum, ?>> {

	public SpectrumEffectHandler() {
		super(Spectrum.class, null);
	}

	@Override
	protected void onStore(Lit component, AbstractBackendEffectController<Spectrum, ?> controller) throws Exception {
	}

}
