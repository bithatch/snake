package uk.co.bithatch.snake.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;

public abstract class AbstractDetailsController extends AbstractDeviceController {

	@FXML
	private Label deviceName;
	@FXML
	private ImageView deviceImage;
	@FXML
	private Hyperlink back;
	@FXML
	private BorderPane header;

	@Override
	protected void onConfigure() {
	}

	@Override
	protected final void onSetDevice() throws Exception {
		header.setBackground(createHeaderBackground());

		deviceImage.setImage(new Image(getDevice().getImage(), true));
		deviceName.textProperty().set(getDevice().getName());
		
		back.visibleProperty().set(!context.getControllers().isEmpty());

		onSetDeviceDetails();
	}

	protected void onSetDeviceDetails() throws Exception {
	}

	@FXML
	void evtBack(ActionEvent evt) {
		context.pop();
	}
}
