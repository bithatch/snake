package uk.co.bithatch.snake.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import uk.co.bithatch.snake.lib.Device;

public class PollRateControl extends ControlController {
	@FXML
	private Label pollRateText;
	@FXML
	private Slider pollRate;

	@Override
	protected void onSetDevice() {
		Device dev = getDevice();
		pollRate.maxProperty().set(2);
		setPollRateForDevice(dev);
		pollRate.setShowTickMarks(true);
		pollRate.setSnapToTicks(true);
		pollRate.setMajorTickUnit(1);
		pollRate.setMinorTickCount(0);
		pollRate.valueProperty().addListener((e) -> {
			int pr;
			switch ((int) pollRate.valueProperty().get()) {
			case 2:
				pr = 1000;
				break;
			case 1:
				pr = 500;
				break;
			default:
				pr = 125;
				break;
			}
			dev.setPollRate(pr);
			pollRateText.textProperty().set(String.format("%dHz", dev.getPollRate()));
		});
		pollRateText.textProperty().set(String.format("%dHz", dev.getPollRate()));
	}

	private void setPollRateForDevice(Device dev) {
		switch (dev.getPollRate()) {
		case 1000:
			pollRate.valueProperty().set(2);
			break;
		case 500:
			pollRate.valueProperty().set(1);
			break;
		default:
			pollRate.valueProperty().set(0);
			break;
		}
	}

}
