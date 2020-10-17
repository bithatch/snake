package uk.co.bithatch.snake.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import uk.co.bithatch.snake.lib.effects.Static;

public class StaticOptions extends AbstractEffectController<Static> {

	@FXML
	private ColorPicker color;

	private boolean adjusting = false;

	@Override
	protected void onConfigure() throws Exception {
		color.valueProperty().addListener((e) -> {
			if (!adjusting) {
				try {
					Static effect = (Static) getEffect().clone();
					effect.setColor(UIHelpers.toRGB(color.valueProperty().get()));
					context.getScheduler().execute(() ->getRegion().setEffect(effect));
				} catch (CloneNotSupportedException cnse) {
					throw new IllegalStateException(cnse);
				}
			}
		});
	}

	@Override
	protected void onSetEffect() {
		Static effect = getEffect();
		adjusting = true;
		try {
			color.valueProperty().set(UIHelpers.toColor(effect.getColor()));
		} finally {
			adjusting = false;
		}
	}

	@FXML
	void evtBack(ActionEvent evt) {
		context.pop();
	}
}
