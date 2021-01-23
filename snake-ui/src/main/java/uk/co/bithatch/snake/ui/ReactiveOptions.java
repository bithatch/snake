package uk.co.bithatch.snake.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import uk.co.bithatch.snake.lib.effects.Reactive;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;
import uk.co.bithatch.snake.ui.effects.ReactiveEffectHandler;
import uk.co.bithatch.snake.widgets.ColorBar;
import uk.co.bithatch.snake.widgets.JavaFX;

public class ReactiveOptions extends AbstractBackendEffectController<Reactive, ReactiveEffectHandler> {

	@FXML
	private Slider speed;

	@FXML
	private ColorBar color;

	private boolean adjusting = false;

	public int getSpeed() {
		return (int) speed.valueProperty().get();
	}

	public int[] getColor() {
		return JavaFX.toRGB(color.getColor());
	}

	@Override
	protected void onConfigure() throws Exception {
		color.colorProperty().addListener((e) -> {
			if (!adjusting) {
				try {
					Reactive effect = (Reactive) getEffect().clone();
					effect.setColor(JavaFX.toRGB(color.getColor()));
					context.getSchedulerManager().get(Queue.DEVICE_IO).execute(() -> getRegion().setEffect(effect));
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
					context.getSchedulerManager().get(Queue.DEVICE_IO).execute(() -> getRegion().setEffect(reactive));
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
