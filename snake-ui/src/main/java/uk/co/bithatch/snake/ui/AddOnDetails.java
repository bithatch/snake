package uk.co.bithatch.snake.ui;

import java.net.URL;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public class AddOnDetails extends AbstractDeviceController {

	@FXML
	private Label author;
	@FXML
	private Label addOnType;
	@FXML
	private Label addOnName;
	@FXML
	private ImageView screenshot;

	@Override
	protected void onConfigure() throws Exception {
	}

	public void setAddOn(AddOn addOn) {
		author.textProperty().set(addOn.getAuthor());
		addOnType.textProperty().set(addOn.getClass().getSimpleName());
		addOnName.textProperty().set(addOn.getName());
		URL ss = addOn.getScreenshot();
		if (ss == null) {
//			screenshot.setImage(new Image(ss, true));
		} else
			screenshot.setImage(new Image(ss.toExternalForm(), true));
	}

	@FXML
	void evtSelect(ActionEvent evt) {
//		context.push(DeviceDetails.class, this, Direction.FROM_RIGHT);
	}

}
