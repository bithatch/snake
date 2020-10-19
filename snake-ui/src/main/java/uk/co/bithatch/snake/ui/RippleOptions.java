package uk.co.bithatch.snake.ui;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import uk.co.bithatch.snake.lib.effects.Ripple;
import uk.co.bithatch.snake.lib.effects.Ripple.Mode;
import uk.co.bithatch.snake.ui.SlideyStack.Direction;

public class RippleOptions extends AbstractEffectController<Ripple> {

	@FXML
	private Slider refreshRate;

	@FXML
	private ColorPicker color;

	@FXML
	private Label colorLabel;

	@FXML
	private RadioButton single;

	@FXML
	private RadioButton random;

	private boolean adjusting = false;

	@Override
	protected void onConfigure() throws Exception {
		color.disableProperty().bind(Bindings.not(single.selectedProperty()));
		colorLabel.setLabelFor(color);
		single.selectedProperty().addListener((e) -> {
			if (single.isSelected())
				setSingle();
		});
		random.selectedProperty().addListener((e) -> {
			if (random.isSelected())
				setRandom();
		});
		color.valueProperty().addListener((e) -> setSingle());
		refreshRate.valueProperty().addListener((e) -> {
			if (!adjusting) {
				try {
					Ripple effect = (Ripple) getEffect().clone();
					effect.setRefreshRate((double) refreshRate.valueProperty().get());
					context.getScheduler().execute(() -> getRegion().setEffect(effect));
				} catch (CloneNotSupportedException cnse) {
					throw new IllegalStateException(cnse);
				}
			}
		});
	}

	protected void setRandom() {
		if (!adjusting) {
			try {
				Ripple breath = (Ripple) getEffect().clone();
				breath.setMode(Mode.RANDOM);
				context.getScheduler().execute(() -> getRegion().setEffect(breath));
			} catch (CloneNotSupportedException cnse) {
				throw new IllegalStateException(cnse);
			}
		}
	}

	protected void setSingle() {
		if (!adjusting) {
			try {
				Ripple breath = (Ripple) getEffect().clone();
				breath.setColor(UIHelpers.toRGB(color.valueProperty().get()));
				breath.setMode(Mode.SINGLE);
				context.getScheduler().execute(() -> getRegion().setEffect(breath));
			} catch (CloneNotSupportedException cnse) {
				throw new IllegalStateException(cnse);
			}
		}
	}

	@FXML
	void evtBack(ActionEvent evt) {
		context.pop();
	}

	@FXML
	void evtOptions(ActionEvent evt) {
		context.push(Options.class, this, Direction.FROM_BOTTOM);
	}

	@Override
	protected void onSetEffect() {
		Ripple effect = getEffect();
		adjusting = true;
		try {
			switch (effect.getMode()) {
			case SINGLE:
				single.selectedProperty().set(true);
				break;
			default:
				random.selectedProperty().set(true);
				break;
			}
			refreshRate.valueProperty().set(effect.getRefreshRate());
			color.valueProperty().set(UIHelpers.toColor(effect.getColor()));
		} finally {
			adjusting = false;
		}
	}

}
