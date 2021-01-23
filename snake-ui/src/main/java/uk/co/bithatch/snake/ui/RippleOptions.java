package uk.co.bithatch.snake.ui;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.effects.Ripple;
import uk.co.bithatch.snake.lib.effects.Ripple.Mode;
import uk.co.bithatch.snake.ui.effects.RippleEffectHandler;
import uk.co.bithatch.snake.widgets.ColorBar;
import uk.co.bithatch.snake.widgets.Direction;
import uk.co.bithatch.snake.widgets.JavaFX;

public class RippleOptions extends AbstractBackendEffectController<Ripple, RippleEffectHandler> {

	@FXML
	private Slider refreshRate;

	@FXML
	private ColorBar color;

	@FXML
	private Label colorLabel;

	@FXML
	private RadioButton single;

	@FXML
	private RadioButton random;

	private boolean adjusting = false;

	public double getRefreshRate() {
		return refreshRate.valueProperty().get();
	}

	public Mode getMode() {
		if (single.selectedProperty().get())
			return Mode.SINGLE;
		else
			return Mode.RANDOM;
	}

	public int[] getColor() {
		return JavaFX.toRGB(color.getColor());
	}

	@Override
	protected void onConfigure() throws Exception {
		color.disableProperty().bind(Bindings.not(single.selectedProperty()));
		colorLabel.setLabelFor(color);
		colorLabel.managedProperty().bind(colorLabel.visibleProperty());
		colorLabel.visibleProperty().bind(color.visibleProperty());
		;
		color.managedProperty().bind(color.visibleProperty());
		color.visibleProperty().bind(single.visibleProperty());
		random.managedProperty().bind(random.visibleProperty());
		single.managedProperty().bind(single.visibleProperty());
		single.selectedProperty().addListener((e) -> {
			if (single.isSelected())
				update();
		});
		random.selectedProperty().addListener((e) -> {
			if (random.isSelected())
				update();
		});
		color.colorProperty().addListener((e) -> update());
		refreshRate.valueProperty().addListener((e) -> update());
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
			color.setColor(JavaFX.toColor(effect.getColor()));
			random.visibleProperty().set(getRegion().getCapabilities().contains(Capability.RIPPLE_RANDOM));
			single.visibleProperty().set(getRegion().getCapabilities().contains(Capability.RIPPLE_SINGLE));
		} finally {
			adjusting = false;
		}
	}

	protected void update() {
		if (!adjusting)
			getEffectHandler().store(getRegion(), this);
	}
}
