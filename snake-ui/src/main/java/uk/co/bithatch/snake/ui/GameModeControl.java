package uk.co.bithatch.snake.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;

public class GameModeControl extends ControlController {
	@FXML
	private Slider gameMode;
	@FXML
	private Label on;
	@FXML
	private Label off;

	@Override
	protected void onSetControlDevice() {
		on.onMouseClickedProperty().set((e) -> gameMode.valueProperty().set(1));
		off.onMouseClickedProperty().set((e) -> gameMode.valueProperty().set(0));
		gameMode.valueProperty().set(getDevice().isGameMode() ? 1 : 0);
		gameMode.valueProperty().addListener((e) -> getDevice().setGameMode(gameMode.valueProperty().get() > 0));
	}

}
