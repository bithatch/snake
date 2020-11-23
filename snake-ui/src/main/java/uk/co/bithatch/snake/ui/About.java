package uk.co.bithatch.snake.ui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import uk.co.bithatch.snake.ui.widgets.Direction;

public class About extends AbstractDeviceController {

	@FXML
	private Label backend;

	@FXML
	private Label backendVersion;

	@FXML
	private Label version;

	@Override
	protected void onConfigure() throws Exception {
		backend.textProperty().set(context.getBackend().getName());
		try {
			backendVersion.textProperty().set(context.getBackend().getVersion());
		} catch (Exception e) {
			backendVersion.textProperty().set("Unknown");
		}
		version.textProperty().set(PlatformService.get().getInstalledVersion());
	}

	@FXML
	void evtBack(ActionEvent evt) {
		context.pop();
	}

	@FXML
	void evtChanges(ActionEvent evt) {
		context.push(Changes.class, Direction.FROM_BOTTOM);
	}
}
