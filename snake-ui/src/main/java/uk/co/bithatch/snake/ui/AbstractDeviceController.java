package uk.co.bithatch.snake.ui;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Device.Listener;
import uk.co.bithatch.snake.ui.util.JavaFX;

public abstract class AbstractDeviceController extends AbstractController implements Listener {

	private Device device;

	@FXML
	private Slider brightness;

	@FXML
	private Label brightnessLabel;

	private boolean adjustingBrightness;

	@Override
	protected void onConfigure() throws Exception {
	}

	public Device getDevice() {
		return device;
	}

	public final void setDevice(Device device) {
		if (this.device != null)
			this.device.removeListener(this);
		this.device = device;
		try {
			if (brightness != null && brightnessLabel != null) {
				JavaFX.bindManagedToVisible(brightness);
				JavaFX.bindManagedToVisible(brightnessLabel);
				brightnessLabel.visibleProperty().bind(brightness.visibleProperty());
				boolean supportsBrightness = device.getCapabilities().contains(Capability.BRIGHTNESS);
				brightness.setVisible(supportsBrightness);
				if (supportsBrightness)
					brightness.valueProperty().set(device.getBrightness());
				else
					brightness.valueProperty().set(0);
				brightness.valueProperty().addListener((e) -> {
					if (!adjustingBrightness) {
						updateBrightness();
					}
				});
			}
			device.addListener(this);
			onSetDevice();
		} catch (Exception e) {
			throw new IllegalStateException(String.format("Failed to configure UI for device %s.", device.getName()),
					e);
		}
	}

	@Override
	protected final void onCleanUp() {
		if (this.device != null)
			device.removeListener(this);
		onDeviceCleanUp();
	}

	protected void onDeviceCleanUp() {
	}

	protected void onSetDevice() throws Exception {
	}

	private void updateBrightness() {
		adjustingBrightness = true;
		try {
			getDevice().setBrightness((short) brightness.valueProperty().get());
		} finally {
			adjustingBrightness = false;
		}
	}

	@Override
	public final void changed(Device device, uk.co.bithatch.snake.lib.Region region) {
		if (!Platform.isFxApplicationThread())
			Platform.runLater(() -> changed(device, region));
		else {
			onChanged(device, region);
			if (!adjustingBrightness && brightness != null
					&& device.getCapabilities().contains(Capability.BRIGHTNESS)) {
				adjustingBrightness = true;
				try {
					brightness.valueProperty().set(device.getBrightness());
				} finally {
					adjustingBrightness = false;
				}
			}
		}

	}

	protected void onChanged(Device device, uk.co.bithatch.snake.lib.Region region) {
	}
}
