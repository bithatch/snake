package uk.co.bithatch.snake.ui;

import javafx.fxml.Initializable;
import javafx.scene.Scene;

public interface Controller extends Initializable {

	Scene getScene();
	void configure(Scene scene, App jfxhsClient);
	void cleanUp();
}
