package uk.co.bithatch.snake.ui;

import java.util.Iterator;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import uk.co.bithatch.macrolib.MacroSystem;
import uk.co.bithatch.macrolib.MacroSystem.RecordingListener;
import uk.co.bithatch.macrolib.RecordedEvent;
import uk.co.bithatch.macrolib.RecordingSession;

public class Record extends AbstractDetailsController implements RecordingListener {

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

	private RecordingSession sequence;
	private MacroSystem macroSystem;

	@Override
	protected void onSetDeviceDetails() throws Exception {
		macroSystem = context.getMacroManager().getMacroSystem();
		sequence = macroSystem.getRecordingSession();

		empty.managedProperty().bind(empty.visibleProperty());

		macroSystem.addRecordingListener(this);
//		headerImageGroup.getChildren().add(MacroMap.iconForMapSequence(sequence));

		updateState();

		rebuildSeq();
	}

	@Override
	protected void onDeviceCleanUp() {
		macroSystem.removeRecordingListener(this);
	}

	private void rebuildSeq() {
		keys.getChildren().clear();
		for (Iterator<RecordedEvent> evtIt = sequence.getEvents(); evtIt.hasNext();) {
			RecordedEvent evt = evtIt.next();
//			Label l = new Label(MacroMap.textForMapAction(m));
//			l.setGraphic(MacroMap.iconForMacro(m));
//			keys.getChildren().add(l);
		}
		empty.visibleProperty().set(keys.getChildren().isEmpty());
	}

	void updateState() {
		switch (sequence.getRecordingState()) {
		case IDLE:
			status.textProperty().set(bundle.getString("state.IDLE"));
			status.getStyleClass().remove("danger");
			status.getStyleClass().remove("warning");
			status.getStyleClass().add("success");
			startRecord.disableProperty().set(false);
			stop.disableProperty().set(true);
			pause.disableProperty().set(true);
			break;
		case WAITING_FOR_EVENTS:
		case WAITING_FOR_TARGET_KEY:
			status.textProperty().set(bundle.getString("state.RECORDING"));
			status.getStyleClass().add("danger");
			status.getStyleClass().remove("warning");
			status.getStyleClass().remove("success");
			startRecord.disableProperty().set(true);
			stop.disableProperty().set(false);
			pause.disableProperty().set(false);
			break;
		case PAUSED:
			status.textProperty().set(bundle.getString("state.RECORDING"));
			status.getStyleClass().remove("danger");
			status.getStyleClass().remove("warning");
			status.getStyleClass().add("success");
			startRecord.disableProperty().set(true);
			stop.disableProperty().set(true);
			pause.disableProperty().set(true);
			break;
		case ERROR:
			notifyMessage(MessageType.DANGER, context.getMacroManager().getMacroSystem().getRecordingSession()
					.getRecordingError().getLocalizedMessage());
			break;
		}
	}

	@FXML
	void evtBack() {
		context.pop();
		if (context.getMacroManager().getMacroSystem().isRecording())
			context.getMacroManager().getMacroSystem().stopRecording();
	}

	@FXML
	void evtStartRecord() {
		context.getMacroManager().getMacroSystem().startRecording();
	}

	@FXML
	void evtPause() {
		context.getMacroManager().getMacroSystem().togglePauseRecording();
	}

	@FXML
	void evtStop() {
		context.getMacroManager().getMacroSystem().stopRecording();
		context.pop();
	}

	@Override
	public void recordingStateChange(RecordingSession session) {
		Platform.runLater(() -> {
			updateState();
			rebuildSeq();
		});
	}

	@Override
	public void eventRecorded() {
		Platform.runLater(() -> rebuildSeq());
	}

}
