package uk.co.bithatch.snake.ui;

import uk.co.bithatch.snake.lib.effects.Matrix;

public class MatrixEffectOptions implements EffectOptions<Matrix> {

	@Override
	public Class<? extends AbstractEffectController<Matrix>> getOptionsController() {
		return MatrixOptions.class;
	}

	@Override
	public Class<Matrix> getEffectClass() {
		return Matrix.class;
	}

}
