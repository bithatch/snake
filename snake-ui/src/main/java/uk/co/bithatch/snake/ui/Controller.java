package uk.co.bithatch.snake.ui;

import javafx.fxml.Initializable;
import javafx.scene.Scene;

public interface Controller extends Initializable {

	public enum MessagePersistence {
		EVERYTIME, ONCE_PER_RUNTIME, ONCE_PER_INSTALL
	}

	public enum MessageType {
		INFO, WARNING, DANGER, SUCCESS
	}

	Scene getScene();

	void configure(Scene scene, App jfxhsClient);

	void cleanUp();

	default void notifyMessage(MessageType messageType, String content) {
		notifyMessage(MessagePersistence.EVERYTIME, messageType, content);
	}

	default void notifyMessage(MessagePersistence persistence, MessageType messageType, String content) {
		notifyMessage(persistence, messageType, null, content);
	}

	default void notifyMessage(MessageType messageType, String title, String content) {
		notifyMessage(MessagePersistence.EVERYTIME, messageType, title, content, 10);
	}

	default void notifyMessage(MessagePersistence persistence, MessageType messageType, String title, String content) {
		notifyMessage(persistence, messageType, title, content, 10);
	}

	void notifyMessage(MessagePersistence persistence, MessageType messageType, String title, String content,
			int timeout);

	default void clearNotifications(boolean immediate) {
		clearNotifications(null, immediate);
	}

	void clearNotifications(MessageType type, boolean immediate);
}
