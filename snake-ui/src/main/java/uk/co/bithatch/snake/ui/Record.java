package uk.co.bithatch.snake.ui;

import java.util.ResourceBundle;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import uk.co.bithatch.snake.lib.binding.MapAction;
import uk.co.bithatch.snake.lib.binding.MapSequence;

public class Record extends AbstractDetailsController {

	public enum State
	{
		IDLE,
		RECORDING,
		PAUSED
	}

	final static ResourceBundle bundle = ResourceBundle.getBundle(Record.class.getName());

	@FXML
	private VBox keyContainer;
	@FXML
	private Label status;
	@FXML
	private Label empty;
	@FXML
	private Hyperlink startRecord;
	@FXML
	private Hyperlink pause;
	@FXML
	private Hyperlink stop;
	@FXML
	private FlowPane keys;
	@FXML
	private Group headerImageGroup;

	private EventHandler<KeyEvent> handler;
	private MapSequence sequence;

	public void setMacroSequence(MapSequence sequence) {
		this.sequence = sequence;

		empty.managedProperty().bind(empty.visibleProperty());

		handler = new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				System.out.println(event);
			}
		};

		headerImageGroup.getChildren().add(MacroMap.iconForMapSequence(sequence));

		if (sequence.isRecording())
			updateState(State.RECORDING);
		else
			updateState(State.IDLE);

		rebuildSeq(sequence);
	}

	private void rebuildSeq(MapSequence seq) {
		keys.getChildren().clear();
		for (MapAction m : seq) {
			Label l = new Label(MacroMap.textForMapAction(m));
			l.setGraphic(MacroMap.iconForMacro(m));
			keys.getChildren().add(l);
		}
		empty.visibleProperty().set(keys.getChildren().isEmpty());
	}

	void updateState(State state) {
		switch (state) {
		case IDLE:
			status.textProperty().set(bundle.getString("state.IDLE"));
			status.getStyleClass().remove("danger");
			status.getStyleClass().remove("warning");
			status.getStyleClass().add("success");
			startRecord.disableProperty().set(false);
			stop.disableProperty().set(true);
			pause.disableProperty().set(true);
			scene.getRoot().setOnKeyPressed(null);
			break;
		case RECORDING:
			status.textProperty().set(bundle.getString("state.RECORDING"));
			status.getStyleClass().add("danger");
			status.getStyleClass().remove("warning");
			status.getStyleClass().remove("success");
			startRecord.disableProperty().set(true);
			stop.disableProperty().set(false);
			pause.disableProperty().set(false);
			scene.getRoot().setOnKeyPressed(handler);
			break;
		case PAUSED:
			status.textProperty().set(bundle.getString("state.RECORDING"));
			status.getStyleClass().remove("danger");
			status.getStyleClass().remove("warning");
			status.getStyleClass().add("success");
			startRecord.disableProperty().set(true);
			stop.disableProperty().set(true);
			pause.disableProperty().set(true);
			scene.getRoot().setOnKeyPressed(null);
			break;
		}
	}

	@FXML
	void evtBack() {
		context.pop();
		if (sequence.isRecording())
			sequence.stopRecording();
	}

	@FXML
	void evtStartRecord() {
		updateState(State.RECORDING);
		sequence.record();
	}

	@FXML
	void evtPause() {
		updateState(State.PAUSED);
		sequence.stopRecording();
	}

	@FXML
	void evtStop() {
		updateState(State.IDLE);
		sequence.stopRecording();
		context.pop();
	}

}
