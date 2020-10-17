package uk.co.bithatch.snake.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import uk.co.bithatch.snake.lib.effects.Reactive;

public class ReactiveOptions extends AbstractEffectController<Reactive> {

	@FXML
	private Slider speed;

	@FXML
	private ColorPicker color;

	private boolean adjusting = false;

	@Override
	protected void onConfigure() throws Exception {
		color.valueProperty().addListener((e) -> {
			if (!adjusting) {
				try {
					Reactive effect = (Reactive) getEffect().clone();
					effect.setColor(UIHelpers.toRGB(color.valueProperty().get()));
					context.getScheduler().execute(() ->getRegion().setEffect(effect));
				} catch (CloneNotSupportedException cnse) {
					throw new IllegalStateException(cnse);
				}
			}
		});
		speed.valueProperty().addListener((e) -> {
			if (!adjusting) {
				try {
					Reactive reactive = (Reactive) getEffect().clone();
					reactive.setSpeed((int) speed.valueProperty().get());
					context.getScheduler().execute(() ->getRegion().setEffect(reactive));
				} catch (CloneNotSupportedException cnse) {
					throw new IllegalStateException(cnse);
				}
			}
		});
	}

	@Override
	protected void onSetEffect() {
		Reactive effect = getEffect();
		adjusting = true;
		try {
			speed.valueProperty().set(effect.getSpeed());
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
