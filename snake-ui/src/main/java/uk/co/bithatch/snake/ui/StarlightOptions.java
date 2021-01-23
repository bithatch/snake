package uk.co.bithatch.snake.ui;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.effects.Starlight;
import uk.co.bithatch.snake.lib.effects.Starlight.Mode;
import uk.co.bithatch.snake.ui.effects.StarlightEffectHandler;
import uk.co.bithatch.snake.widgets.ColorBar;
import uk.co.bithatch.snake.widgets.Direction;
import uk.co.bithatch.snake.widgets.JavaFX;

public class StarlightOptions extends AbstractBackendEffectController<Starlight, StarlightEffectHandler> {
	@FXML
	private ColorBar color;

	@FXML
	private ColorBar color1;

	@FXML
	private ColorBar color2;

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

	public int getSpeed() {
		return (int) speed.valueProperty().get();
	}

	public Mode getMode() {
		if (single.selectedProperty().get())
			return Mode.SINGLE;
		else if (dual.selectedProperty().get())
			return Mode.DUAL;
		else
			return Mode.RANDOM;
	}

	public int[] getColor() {
		return JavaFX.toRGB(color.getColor());
	}

	public int[] getColor1() {
		return JavaFX.toRGB(color1.getColor());
	}

	public int[] getColor2() {
		return JavaFX.toRGB(color2.getColor());
	}

	protected void update() {
		if (!adjusting)
			getEffectHandler().store(getRegion(), this);
	}

	@Override
	protected void onConfigure() throws Exception {

		colorLabel.managedProperty().bind(colorLabel.visibleProperty());
		color1Label.managedProperty().bind(color1Label.visibleProperty());
		color2Label.managedProperty().bind(color2Label.visibleProperty());
		random.managedProperty().bind(random.visibleProperty());
		single.managedProperty().bind(single.visibleProperty());
		dual.managedProperty().bind(dual.visibleProperty());
		color.managedProperty().bind(color.visibleProperty());

		colorLabel.visibleProperty().bind(single.visibleProperty());
		color1Label.visibleProperty().bind(color1.visibleProperty());
		color2Label.visibleProperty().bind(color2.visibleProperty());

		colorLabel.setLabelFor(color);
		color1Label.setLabelFor(color1);
		color2Label.setLabelFor(color2);

		color.disableProperty().bind(Bindings.not(single.selectedProperty()));
		color1.disableProperty().bind(Bindings.not(dual.selectedProperty()));
		color2.disableProperty().bind(Bindings.not(dual.selectedProperty()));

		color.visibleProperty().bind(single.visibleProperty());
		color1.visibleProperty().bind(dual.visibleProperty());
		color2.visibleProperty().bind(dual.visibleProperty());

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
		color.colorProperty().addListener((e) -> update());
		color1.colorProperty().addListener((e) -> update());
		color2.colorProperty().addListener((e) -> update());
		speed.valueProperty().addListener((e) -> {
			update();
		});
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
			color.setColor(JavaFX.toColor(effect.getColor()));
			color1.setColor(JavaFX.toColor(effect.getColor1()));
			color2.setColor(JavaFX.toColor(effect.getColor2()));
			speed.valueProperty().set(effect.getSpeed());
			random.visibleProperty().set(getRegion().getCapabilities().contains(Capability.STARLIGHT_RANDOM));
			single.visibleProperty().set(getRegion().getCapabilities().contains(Capability.STARLIGHT_SINGLE));
			dual.visibleProperty().set(getRegion().getCapabilities().contains(Capability.STARLIGHT_DUAL));
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
