package uk.co.bithatch.snake.ui;

import uk.co.bithatch.snake.lib.effects.Reactive;

public class ReactiveEffectOptions implements EffectOptions<Reactive> {

	@Override
	public Class<? extends AbstractEffectController<Reactive>> getOptionsController() {
		return ReactiveOptions.class;
	}

	@Override
	public Class<Reactive> getEffectClass() {
		return Reactive.class;
	}

}
