package uk.co.bithatch.snake.ui;

public abstract class ControlController extends AbstractDeviceController {

	@Override
	protected final void onSetDevice() throws Exception {
		getScene().getRoot().getStyleClass().add(getControlClassName());
		getScene().getRoot().getStyleClass().add(getControlClassName() + "-" + getClass().getSimpleName().toLowerCase());
		onSetControlDevice();
	}

	protected String getControlClassName() {
		return "control";
	}

	protected void onSetControlDevice() throws Exception {

	}
}
