package uk.co.bithatch.snake.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import uk.co.bithatch.snake.lib.effects.Wave;
import uk.co.bithatch.snake.lib.effects.Wave.Direction;

public class WaveOptions extends AbstractEffectController<Wave> {

	@FXML
	private Slider direction;

	private boolean adjusting = false;

	@Override
	protected void onConfigure() throws Exception {
		direction.valueProperty().addListener((e) -> {
			if (!adjusting) {
				try {
					Wave breath = (Wave) getEffect().clone();
					Direction dir = uk.co.bithatch.snake.lib.effects.Wave.Direction
							.values()[(int) direction.valueProperty().get()];
					breath.setDirection(dir);
					context.getScheduler().execute(() -> getRegion().setEffect(breath));
				} catch (CloneNotSupportedException cnse) {
					throw new IllegalStateException(cnse);
				}
			}
		});
	}

	@Override
	protected void onSetEffect() {
		Wave effect = getEffect();
		adjusting = true;
		try {
			direction.valueProperty().set(effect.getDirection().ordinal());
		} finally {
			adjusting = false;
		}
	}

	@FXML
	void evtBack(ActionEvent evt) {
		context.pop();
	}

	@FXML
	void evtBackward(MouseEvent evt) {
		direction.valueProperty().set(0.0);
	}

	@FXML
	void evtForward(MouseEvent evt) {
		direction.valueProperty().set(1.0);
	}

}
