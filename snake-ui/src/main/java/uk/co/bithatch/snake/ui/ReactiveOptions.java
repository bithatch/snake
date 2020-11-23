package uk.co.bithatch.snake.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Slider;
import uk.co.bithatch.snake.lib.effects.Reactive;
import uk.co.bithatch.snake.ui.effects.ReactiveEffectHandler;
import uk.co.bithatch.snake.ui.util.JavaFX;

public class ReactiveOptions extends AbstractBackendEffectController<Reactive, ReactiveEffectHandler> {

	@FXML
	private Slider speed;

	@FXML
	private ColorPicker color;

	private boolean adjusting = false;

	public int getSpeed() {
		return (int) speed.valueProperty().get();
	}

	public int[] getColor() {
		return JavaFX.toRGB(color.valueProperty().get());
	}

	@Override
	protected void onConfigure() throws Exception {
		color.valueProperty().addListener((e) -> {
			if (!adjusting) {
				try {
					Reactive effect = (Reactive) getEffect().clone();
					effect.setColor(JavaFX.toRGB(color.valueProperty().get()));
					context.getScheduler().execute(() -> getRegion().setEffect(effect));
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
					context.getScheduler().execute(() -> getRegion().setEffect(reactive));
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
