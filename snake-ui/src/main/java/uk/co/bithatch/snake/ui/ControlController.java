package uk.co.bithatch.snake.ui;

public abstract class ControlController extends AbstractDeviceController {

	@Override
	protected final void onSetDevice() throws Exception {
		getScene().getRoot().getStyleClass().add("control");
		getScene().getRoot().getStyleClass().add("control-" + getClass().getSimpleName().toLowerCase());
		onSetControlDevice();
	}

	protected void onSetControlDevice() throws Exception {

	}
}
