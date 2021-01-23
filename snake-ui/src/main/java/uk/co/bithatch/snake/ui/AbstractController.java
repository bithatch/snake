package uk.co.bithatch.snake.ui;

import java.lang.System.Logger.Level;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;

import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;
import uk.co.bithatch.snake.ui.util.Strings;

public abstract class AbstractController implements Controller {

	final static System.Logger LOG = System.getLogger(AbstractController.class.getName());

	protected App context;
	protected ResourceBundle resources;
	protected URL location;
	protected Scene scene;

	private static Set<String> shownMessages = new HashSet<>();

	@FXML
	private VBox popupMessages;

	{
		if (popupMessages != null) {
			popupMessages.getStyleClass().add("popupMessages");
			popupMessages.getStyleClass().add("padded");
		}
	}

	@Override
	public final void initialize(URL location, ResourceBundle resources) {
		this.location = location;
		this.resources = resources;
		onInitialize();
	}

	@Override
	public final void cleanUp() {
		onCleanUp();
	}

	@Override
	public final void configure(Scene scene, App jfxhsClient) {
		this.scene = scene;
		this.context = jfxhsClient;
		try {
			onConfigure();
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	public void error(Exception exception) {
		if (exception == null)
			clearNotifications(true);
		else {
			LOG.log(Level.ERROR, "Error.", exception);
			String msg = exception.getLocalizedMessage();
			notifyMessage(MessagePersistence.EVERYTIME, MessageType.DANGER, msg);
		}
	}

	@Override
	public void notifyMessage(MessagePersistence persistence, MessageType messageType, String title, String content,
			int timeout) {
		String key = Strings.genericHash(
				messageType.name() + "-" + (title == null ? "none" : title) + "-" + (content == null ? "" : content));
		Preferences node = context.getPreferences().node("shownMessages");
		if (persistence == MessagePersistence.ONCE_PER_RUNTIME && shownMessages.contains(key)) {
			return;
		} else if (persistence == MessagePersistence.ONCE_PER_INSTALL && node.getBoolean(key, false)) {
			return;
		}
		shownMessages.add(key);
		node.putBoolean(key, true);

		if (Platform.isFxApplicationThread()) {
			if (popupMessages == null)
				LOG.log(Level.WARNING, String.format("%s does not support popups", getClass().getName()));
			else {
				popupMessages.getChildren().add(new Message(messageType, title, content, timeout));
			}
		} else
			Platform.runLater(() -> notifyMessage(persistence, messageType, title, content, timeout));

	}

	@Override
	public void clearNotifications(MessageType type, boolean immediate) {

		if (popupMessages == null)
			return;
		if (Platform.isFxApplicationThread()) {
			if (immediate) {
				for (Node n : new ArrayList<>(popupMessages.getChildren())) {
					if (type == null || ((Message) n).type == type) {
						popupMessages.getChildren().remove(n);
					}
				}
			} else {
				for (Node n : popupMessages.getChildren()) {
					if (type == null || ((Message) n).type == type) {
						((Message) n).close();
					}
				}
			}
		} else
			Platform.runLater(() -> clearNotifications(immediate));
	}

	protected Stage getStage() {
		return (Stage) scene.getWindow();
	}

	protected Background createHeaderBackground() {
		return new Background(
				new BackgroundImage(
						new Image(context.getConfiguration().getTheme().getResource("fibre.jpg").toExternalForm(),
								true),
						BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
						new BackgroundSize(100d, 100d, true, true, false, true)));
	}

	protected void onConfigure() throws Exception {
	}

	protected void onCleanUp() {
	}

	protected void onInitialize() {
	}

	@Override
	public Scene getScene() {
		return scene;
	}

	class Message extends BorderPane {

		private ScheduledFuture<?> task;
		private MessageType type;

		Message(MessageType type, String title, String content, int timeout) {
			this.type = type;

			getStyleClass().add(type.name().toLowerCase());
			getStyleClass().add("spaced");
			getStyleClass().add("padded");
			getStyleClass().add("popupMessages");

			HBox l2 = new HBox();
			l2.setAlignment(Pos.CENTER_LEFT);
			l2.getStyleClass().add("row");
			l2.getStyleClass().add("popupMessageContent");

			Label l1 = new Label();
			HBox.setHgrow(l1, Priority.ALWAYS);
			l1.setEllipsisString("");
			l1.setAlignment(Pos.CENTER);
			l1.getStyleClass().add("icon");
			l1.getStyleClass().add("popupMessageIcon");
			l1.graphicProperty().set(new FontIcon(App.BUNDLE.getString("messageType." + type.name())));
			l2.getChildren().add(l1);

			if (title != null) {
				Label c1 = new Label();
				HBox.setHgrow(c1, Priority.ALWAYS);
				c1.getStyleClass().add("emphasis");
				c1.getStyleClass().add("popupMessageTitle");
				c1.textProperty().set(title);
				c1.setAlignment(Pos.CENTER_LEFT);
				l2.getChildren().add(c1);
			}

			if (content != null) {
				Label c2 = new Label();
				c2.textProperty().set(content);
				c2.getStyleClass().add("popupMessageContent");
				c2.setAlignment(Pos.CENTER_LEFT);
				c2.setWrapText(true);
				l2.getChildren().add(c2);
			}

			Hyperlink l3 = new Hyperlink();
			l3.setGraphic(new FontIcon(FontAwesome.CLOSE));
			l3.getStyleClass().add("icon");
			l3.getStyleClass().add("popupMessageClose");
			l3.setAlignment(Pos.CENTER);
			l3.setOnMouseClicked((e) -> {
				if (task != null)
					task.cancel(false);
				close();
			});

			setCenter(l2);
			setRight(l3);

			if (timeout != 0)
				task = context.getSchedulerManager().get(Queue.TIMER).schedule(() -> {
					Platform.runLater(() -> close());
				}, timeout, TimeUnit.SECONDS);
		}

		void close() {

			ScaleTransition st = new ScaleTransition(Duration.millis(250), Message.this);
			st.setFromY(1);
			st.setToY(0);
			st.setCycleCount(1);
			st.onFinishedProperty().set((e2) -> {
				popupMessages.getChildren().remove(Message.this);
			});
			st.play();
		}
	}
}
