package uk.co.bithatch.snake.ui.widgets;

import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import uk.co.bithatch.snake.ui.graphics.LEDGraphic;
import uk.co.bithatch.snake.ui.util.JavaFX;

public abstract class AbstractProfileLEDs extends HBox {

	private LEDGraphic blue;
	private LEDGraphic green;
	private LEDGraphic red;
	private boolean adjusting;

	public AbstractProfileLEDs() {

		red = new LEDGraphic();
		red.setLedColor(Color.RED);
		red.setOnMouseClicked((e) -> {
			if (!adjusting) {
				boolean[] leds = getLEDState();
				leds[0] = !leds[0];
				setLEDState(leds);
				JavaFX.glowOrDeemphasis(red, leds[0]);
			}
		});
		green = new LEDGraphic();
		green.setLedColor(Color.GREEN);
		green.setOnMouseClicked((e) -> {
			if (!adjusting) {
				boolean[] leds = getLEDState();
				leds[1] = !leds[1];
				setLEDState(leds);
				JavaFX.glowOrDeemphasis(green, leds[1]);
			}
		});
		blue = new LEDGraphic();
		blue.setLedColor(Color.BLUE);
		blue.setOnMouseClicked((e) -> {
			if (!adjusting) {
				boolean[] leds = getLEDState();
				leds[2] = !leds[2];
				setLEDState(leds);
				JavaFX.glowOrDeemphasis(blue, leds[2]);
			}
		});
		getChildren().add(red);
		getChildren().add(green);
		getChildren().add(blue);
		setRGB(new boolean[3]);
	}

	public void setRGB(boolean[] rgb) {
		adjusting = true;
		try {
			red.selectedProperty().set(rgb[0]);
			JavaFX.glowOrDeemphasis(red, rgb[0]);
			green.selectedProperty().set(rgb[1]);
			JavaFX.glowOrDeemphasis(green, rgb[1]);
			blue.selectedProperty().set(rgb[2]);
			JavaFX.glowOrDeemphasis(blue, rgb[2]);
		} finally {
			adjusting = false;
		}
	}

	protected abstract void setLEDState(boolean[] state);

	protected abstract boolean[] getLEDState();
}
