package uk.co.bithatch.snake.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import uk.co.bithatch.snake.lib.effects.Static;
import uk.co.bithatch.snake.ui.effects.StaticEffectHandler;
import uk.co.bithatch.snake.widgets.ColorBar;
import uk.co.bithatch.snake.widgets.JavaFX;

public class StaticOptions extends AbstractBackendEffectController<Static, StaticEffectHandler> {

	@FXML
	private ColorBar color;

	private boolean adjusting = false;

	public int[] getColor() {
		return JavaFX.toRGB(color.getColor());
	}

	@Override
	protected void onConfigure() throws Exception {
		color.colorProperty().addListener((e) -> {
			if (!adjusting) {
				getEffectHandler().store(getRegion(), this);
			}
		});
	}

	@Override
	protected void onSetEffect() {
		Static effect = getEffect();
		adjusting = true;
		try {
			color.setColor(JavaFX.toColor(effect.getColor()));
		} finally {
			adjusting = false;
		}
	}

	@FXML
	void evtBack(ActionEvent evt) {
		context.pop();
	}
}
