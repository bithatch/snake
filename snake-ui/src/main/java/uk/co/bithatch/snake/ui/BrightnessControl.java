package uk.co.bithatch.snake.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Region;

public class BrightnessControl extends ControlController {
	@FXML
	private Label brightnessText;
	@FXML
	private Slider brightness;
	@FXML
	private VBox regions;

	private boolean adjustingOverall = false;
	private boolean adjustingSingle = false;

	@Override
	protected void onSetDevice() {

		List<Slider> others = new ArrayList<>();

		brightness.maxProperty().set(100);
		brightness.valueProperty().set(getDevice().getBrightness());
		brightness.setShowTickMarks(true);
		brightness.setSnapToTicks(true);
		brightness.setMajorTickUnit(10);
		brightness.setMinorTickCount(1);
		brightness.valueProperty().addListener((e) -> {
			if (!adjustingOverall) {
				getDevice().setBrightness((short) brightness.valueProperty().get());
				brightnessText.textProperty().set(String.format("%d%%", getDevice().getBrightness()));
				adjustingSingle = true;
				try {
					for (Slider sl : others) {
						sl.setValue(brightness.valueProperty().get());
					}
				} finally {
					adjustingSingle = false;
				}
			}
		});
		brightnessText.textProperty().set(String.format("%d%%", getDevice().getBrightness()));

		if (getDevice().getCapabilities().contains(Capability.BRIGHTNESS_PER_REGION)) {
			List<Region> regionList = getDevice().getRegions();
			if (regionList.size() > 1) {
				for (Region r : regionList) {
					if (r.getCapabilities().contains(Capability.BRIGHTNESS_PER_REGION)) {
						HBox hbox = new HBox();

						ImageView iv = new ImageView(context.getConfiguration().themeProperty().getValue().getRegionImage(24, r.getName()).toExternalForm());
						iv.setFitHeight(22);
						iv.setFitWidth(22);
						iv.setSmooth(true);
						iv.setPreserveRatio(true);

						Slider br = new Slider(0, 100, r.getBrightness());
						br.maxWidth(80);
						Label la = new Label(String.format("%d%%", r.getBrightness()));
						la.getStyleClass().add("small");

						br.valueProperty().addListener((e) -> {
							r.setBrightness((short) br.valueProperty().get());
							if (!adjustingSingle) {
								adjustingOverall = true;
								try {
									brightness.setValue(getDevice().getBrightness());
								} finally {
									adjustingOverall = false;
								}
							}
							la.textProperty().set(String.format("%d%%", r.getBrightness()));
						});
						others.add(br);

						hbox.getChildren().add(iv);
						hbox.getChildren().add(br);
						hbox.getChildren().add(la);
						regions.getChildren().add(hbox);
					}
				}
			}
		}
	}

	void setBrightness(short brightness) {
		for (Region r : getDevice().getRegions()) {
			if (r.getCapabilities().contains(Capability.BRIGHTNESS_PER_REGION))
				r.setBrightness(brightness);
		}
	}

}
