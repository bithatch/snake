package uk.co.bithatch.snake.ui;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import uk.co.bithatch.snake.lib.effects.Breath;
import uk.co.bithatch.snake.lib.effects.Breath.Mode;
import uk.co.bithatch.snake.ui.effects.BreathEffectHandler;
import uk.co.bithatch.snake.ui.util.JavaFX;
import uk.co.bithatch.snake.ui.widgets.Direction;

public class BreathOptions extends AbstractBackendEffectController<Breath, BreathEffectHandler> {

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

	private boolean adjusting = false;

	public Mode getMode() {
		if (single.selectedProperty().get())
			return Mode.SINGLE;
		else if (dual.selectedProperty().get())
			return Mode.DUAL;
		else
			return Mode.RANDOM;
	}

	public int[] getColor() {
		return JavaFX.toRGB(color.valueProperty().get());
	}

	public int[] getColor1() {
		return JavaFX.toRGB(color1.valueProperty().get());
	}

	public int[] getColor2() {
		return JavaFX.toRGB(color2.valueProperty().get());
	}

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
				update();
		});
		single.selectedProperty().addListener((e) -> {
			if (single.isSelected())
				update();
		});
		random.selectedProperty().addListener((e) -> {
			if (random.isSelected())
				update();
		});
		color.valueProperty().addListener((e) -> update());
		color1.valueProperty().addListener((e) -> update());
		color2.valueProperty().addListener((e) -> update());
	}

	protected void update() {
		if(!adjusting)
			getEffectHandler().store(getRegion(), BreathOptions.this);
	}

	@Override
	protected void onSetEffect() {
		Breath effect = getEffect();
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
			color.valueProperty().set(JavaFX.toColor(effect.getColor()));
			color1.valueProperty().set(JavaFX.toColor(effect.getColor1()));
			color2.valueProperty().set(JavaFX.toColor(effect.getColor2()));
		} finally {
			adjusting = false;
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
}
