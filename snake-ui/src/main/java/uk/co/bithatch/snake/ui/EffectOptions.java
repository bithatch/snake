package uk.co.bithatch.snake.ui;

import uk.co.bithatch.snake.lib.effects.Effect;

public interface EffectOptions<E extends Effect> {

	Class<? extends AbstractEffectController<E>> getOptionsController();

	Class<E> getEffectClass();
}
