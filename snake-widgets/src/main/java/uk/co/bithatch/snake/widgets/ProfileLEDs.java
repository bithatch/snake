package uk.co.bithatch.snake.widgets;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;

public class ProfileLEDs extends HBox {

	private LEDGraphic blue;
	private LEDGraphic green;
	private LEDGraphic red;
	private boolean adjusting;
	private ObjectProperty<boolean[]> rgbs = new SimpleObjectProperty<>(null, "rgbs");

	public ProfileLEDs() {

		red = new LEDGraphic();
		red.setLedColor(Color.RED);
		red.setOnMouseClicked((e) -> {
			if (!adjusting) {
				boolean[] leds = getRgbs();
				setRgbs(new boolean[] { !leds[0], leds[1], leds[2] });
			}
		});
		green = new LEDGraphic();
		green.setLedColor(Color.GREEN);
		green.setOnMouseClicked((e) -> {
			if (!adjusting) {
				boolean[] leds = getRgbs();
				setRgbs(new boolean[] { leds[0], !leds[1], leds[2] });
			}
		});
		blue = new LEDGraphic();
		blue.setLedColor(Color.BLUE);
		blue.setOnMouseClicked((e) -> {
			if (!adjusting) {
				boolean[] leds = getRgbs();
				setRgbs(new boolean[] { leds[0], leds[1], !leds[2] });
			}
		});
		getChildren().add(red);
		getChildren().add(green);
		getChildren().add(blue);

		rgbs.addListener((c, o, n) -> {
			adjusting = true;
			try {
				updateLEDs(n);
			} finally {
				adjusting = false;
			}
		});

		setRgbs(new boolean[3]);
		
	}

	protected void updateLEDs(boolean[] n) {
		red.setSelected(n[0]);
		JavaFX.glowOrDeemphasis(red, n[0]);
		green.setSelected(n[1]);
		JavaFX.glowOrDeemphasis(green, n[1]);
		blue.setSelected(n[2]);
		JavaFX.glowOrDeemphasis(blue, n[2]);
	}

	public ObjectProperty<boolean[]> rgbs() {
		return rgbs;
	}

	public boolean[] getRgbs() {
		return rgbs.get();
	}

	public void setRgbs(boolean[] rgbs) {
		this.rgbs.set(rgbs);
		updateLEDs(rgbs);
	}

}
