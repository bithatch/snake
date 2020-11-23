package uk.co.bithatch.snake.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

public class DPIControl extends ControlController {
	@FXML
	private Label dpiText;
	@FXML
	private Slider dpi;

	@Override
	protected void onSetControlDevice() {
		dpi.maxProperty().set(getDevice().getMaxDPI());
		dpi.valueProperty().set(getDevice().getDPI()[0]);
		dpi.snapToTicksProperty().set(true);
		dpi.majorTickUnitProperty().set(1000);
		dpi.minorTickCountProperty().set(100);
		dpi.valueProperty().addListener((e) -> {
			short v = (short) ((short) ((float) dpi.valueProperty().get() / 100.0) * (short) 100);
			getDevice().setDPI(v, v);
			dpiText.textProperty().set(String.format("%d", getDevice().getDPI()[0]));
		});
		dpiText.textProperty().set(String.format("%d", getDevice().getDPI()[0]));
	}

}
