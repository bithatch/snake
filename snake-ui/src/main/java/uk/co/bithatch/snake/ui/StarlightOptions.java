package uk.co.bithatch.snake.ui;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import uk.co.bithatch.snake.lib.effects.Starlight;
import uk.co.bithatch.snake.lib.effects.Starlight.Mode;
import uk.co.bithatch.snake.ui.SlideyStack.Direction;

public class StarlightOptions extends AbstractEffectController<Starlight> {
	@FXML
	private ColorPicker color;

	@FXML
	private ColorPicker color1;

	@FXML
	private ColorPicker color2;

	@FXML
	private Label colorLabel;

	@FXML
	private Label color1Label;

	@FXML
	private Label color2Label;

	@FXML
	private RadioButton single;

	@FXML
	private RadioButton dual;

	@FXML
	private RadioButton random;
	@FXML
	private Slider speed;
	@FXML
	private ToggleGroup mode;

	private boolean adjusting = false;

	@Override
	protected void onConfigure() throws Exception {
		color.disableProperty().bind(Bindings.not(single.selectedProperty()));
		color1.disableProperty().bind(Bindings.not(dual.selectedProperty()));
		color2.disableProperty().bind(Bindings.not(dual.selectedProperty()));
		colorLabel.setLabelFor(color);
		color1Label.setLabelFor(color1);
		color2Label.setLabelFor(color2);
		dual.selectedProperty().addListener((e) -> {
			if (dual.isSelected())
				setDual();
		});
		single.selectedProperty().addListener((e) -> {
			if (single.isSelected())
				setSingle();
		});
		random.selectedProperty().addListener((e) -> {
			if (random.isSelected())
				setRandom();
		});
		color.valueProperty().addListener((e) -> setSingle());
		color1.valueProperty().addListener((e) -> setDual());
		color2.valueProperty().addListener((e) -> setDual());
		speed.valueProperty().addListener((e) -> {
			if (!adjusting) {
				try {
					Starlight reactive = (Starlight) getEffect().clone();
					reactive.setSpeed((int) speed.valueProperty().get());
					context.getScheduler().execute(() -> getRegion().setEffect(reactive));
				} catch (CloneNotSupportedException cnse) {
					throw new IllegalStateException(cnse);
				}
			}
		});
	}

	protected void setRandom() {
		if (!adjusting) {
			try {
				Starlight breath = (Starlight) getEffect().clone();
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
				Starlight breath = (Starlight) getEffect().clone();
				breath.setColor(UIHelpers.toRGB(color.valueProperty().get()));
				breath.setMode(Mode.SINGLE);
				context.getScheduler().execute(() -> getRegion().setEffect(breath));
			} catch (CloneNotSupportedException cnse) {
				throw new IllegalStateException(cnse);
			}
		}
	}

	protected void setDual() {
		if (!adjusting) {
			try {
				Starlight breath = (Starlight) getEffect().clone();
				breath.setColor1(UIHelpers.toRGB(color1.valueProperty().get()));
				breath.setColor2(UIHelpers.toRGB(color1.valueProperty().get()));
				breath.setMode(Mode.DUAL);
				context.getScheduler().execute(() -> getRegion().setEffect(breath));
			} catch (CloneNotSupportedException cnse) {
				throw new IllegalStateException(cnse);
			}
		}
	}

	@Override
	protected void onSetEffect() {
		Starlight effect = getEffect();
		adjusting = true;
		try {
			switch (effect.getMode()) {
			case DUAL:
				dual.selectedProperty().set(true);
				break;
			case SINGLE:
				single.selectedProperty().set(true);
				break;
			default:
				random.selectedProperty().set(true);
				break;
			}
			color.valueProperty().set(UIHelpers.toColor(effect.getColor()));
			color1.valueProperty().set(UIHelpers.toColor(effect.getColor1()));
			color2.valueProperty().set(UIHelpers.toColor(effect.getColor2()));
			speed.valueProperty().set(effect.getSpeed());
		} finally {
			adjusting = false;
		}
	}

	@FXML
	void evtOptions(ActionEvent evt) {
		context.push(Options.class, this, Direction.FROM_BOTTOM);
	}

	@FXML
	void evtBack(ActionEvent evt) {
		context.pop();
	}

}
