package uk.co.bithatch.snake.ui;

import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import uk.co.bithatch.snake.lib.Device;

public abstract class AbstractDeviceController extends AbstractController {

	private Device device;

	@Override
	protected void onConfigure() throws Exception {
	}

	public Device getDevice() {
		return device;
	}

	public final void setDevice(Device device) {
		this.device = device;
		try {
			onSetDevice();
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Failed to configure UI for device %s.", device.getName()), e);
		}
	}

	protected void onSetDevice() throws Exception {
	}

	protected Background createHeaderBackground() {
		return new Background(
				new BackgroundImage(new Image(getClass().getResource("fibre.jpg").toExternalForm(), true),
						BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
						new BackgroundSize(100d, 100d, true, true, false, true)));
	}
}
