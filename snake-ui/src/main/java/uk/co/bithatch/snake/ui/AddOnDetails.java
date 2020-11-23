package uk.co.bithatch.snake.ui;

import java.net.URL;

import com.sshtools.icongenerator.IconBuilder;
import com.sshtools.icongenerator.IconBuilder.AwesomeIconMode;
import com.sshtools.icongenerator.IconBuilder.TextContent;

import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import uk.co.bithatch.snake.ui.addons.AddOn;

public class AddOnDetails extends AbstractDeviceController {

	@FXML
	private Label author;
	@FXML
	private Label addOnType;
	@FXML
	private Label addOnName;
	@FXML
	private Label screenshot;

	@Override
	protected void onConfigure() throws Exception {
	}

	public void setAddOn(AddOn addOn) {
		author.textProperty().set(addOn.getAuthor());
		addOnType.textProperty().set(AddOns.bundle.getString("addOnType." + addOn.getClass().getSimpleName()));
		addOnName.textProperty().set(addOn.getName());
		URL ss = addOn.getScreenshot();
		if (ss == null) {

			IconBuilder builder = new IconBuilder();
			builder.width(96);
			builder.height(96);
			builder.text(addOn.getName());
			builder.autoShape();
			builder.autoColor();
			builder.textContent(TextContent.INITIALS);
			builder.autoTextColor();
			builder.awesomeIconMode(AwesomeIconMode.AUTO_MATCH);
			screenshot.setGraphic(builder.build(Canvas.class));
		} else {
			ImageView iv = new ImageView(new Image(ss.toExternalForm(), true));
			iv.setFitHeight(96);
			iv.setFitWidth(96);
			screenshot.setGraphic(iv);
		}
	}

	@FXML
	void evtSelect() {
//		context.push(DeviceDetails.class, this, Direction.FROM_RIGHT);
	}

}
