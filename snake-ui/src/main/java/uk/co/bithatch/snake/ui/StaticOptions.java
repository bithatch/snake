package uk.co.bithatch.snake.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import uk.co.bithatch.snake.lib.effects.Static;
import uk.co.bithatch.snake.ui.effects.StaticEffectHandler;
import uk.co.bithatch.snake.ui.util.JavaFX;

public class StaticOptions extends AbstractBackendEffectController<Static, StaticEffectHandler> {

	@FXML
	private ColorPicker color;

	private boolean adjusting = false;

	public int[] getColor() {
		return JavaFX.toRGB(color.valueProperty().get());
	}

	@Override
	protected void onConfigure() throws Exception {
		color.valueProperty().addListener((e) -> {
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
			color.valueProperty().set(JavaFX.toColor(effect.getColor()));
		} finally {
			adjusting = false;
		}
	}

	@FXML
	void evtBack(ActionEvent evt) {
		context.pop();
	}
}
