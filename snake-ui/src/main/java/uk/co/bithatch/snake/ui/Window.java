package uk.co.bithatch.snake.ui;

import org.apache.commons.lang3.SystemUtils;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import uk.co.bithatch.snake.ui.SlideyStack.Direction;

public class Window extends AbstractController {

//	public static Font fxont;
//
//	static {
//		foxnt = Font.loadFont(AwesomeIcons.class.getResource("FROSTBITE-Narrow.ttf").toExternalForm(), 12);
//	}

	@FXML
	private Hyperlink min;
	@FXML
	private Hyperlink max;
	@FXML
	private Hyperlink close;
	@FXML
	private Label title;
	@FXML
	private BorderPane bar;
	@FXML
	private BorderPane titleBar;
	@FXML
	private StackPane content;
	@FXML
	private Hyperlink options;
	@FXML
	private ImageView titleBarImage;

	private Rectangle2D bounds;

	@Override
	protected void onConfigure() throws Exception {
		titleBarImage.setImage(new Image(context.getConfiguration().themeProperty().getValue().getResource("icons/app64.png")
				.toExternalForm(), true));
		titleBar.setBackground(new Background(new BackgroundImage(
				new Image(context.getConfiguration().themeProperty().getValue().getResource("titleBar.png")
						.toExternalForm(), true),
				BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
				new BackgroundSize(100d, 100d, true, true, false, true))));
	}

	public Hyperlink getOptions() {
		return options;
	}

	public BorderPane getBar() {
		return bar;
	}

	public StackPane getContent() {
		return content;
	}

	@FXML
	void evtMin(ActionEvent evt) {
		Stage stage = (Stage) ((Hyperlink) evt.getSource()).getScene().getWindow();
		stage.setIconified(true);
	}

	@FXML
	void evtMax(ActionEvent evt) {
		Stage stage = (Stage) ((Hyperlink) evt.getSource()).getScene().getWindow();
		if (SystemUtils.IS_OS_LINUX) {
			if (!stage.isMaximized()) {

				this.bounds = new Rectangle2D(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight());

				stage.setMaximized(true);

				/*
				 * Work around, maximize STIL doesn't work on Linux properly.
				 * https://stackoverflow.com/questions/6864540/how-to-set-a-javafx-stage-frame-
				 * to-maximized
				 */
				// Get current screen of the stage
				ObservableList<Screen> screens = Screen.getScreensForRectangle(this.bounds);

				// Change stage properties
				Rectangle2D newBounds = screens.get(0).getVisualBounds();
				stage.setX(newBounds.getMinX());
				stage.setY(newBounds.getMinY());
				stage.setWidth(newBounds.getWidth());
				stage.setHeight(newBounds.getHeight());
			} else {
				stage.setMaximized(false);
				stage.setX(bounds.getMinX());
				stage.setY(bounds.getMinY());
				stage.setWidth(bounds.getWidth());
				stage.setHeight(bounds.getHeight());
			}

		} else
			stage.setMaximized(!stage.isMaximized());
	}

	@FXML
	void evtClose(ActionEvent evt) {
		context.close();
	}

	@FXML
	void evtAbout(ActionEvent evt) {
		context.push(About.class, Direction.FADE_IN);
	}

	@FXML
	void evtOptions(ActionEvent evt) {
		context.push(Options.class, Direction.FROM_BOTTOM);
	}
}
