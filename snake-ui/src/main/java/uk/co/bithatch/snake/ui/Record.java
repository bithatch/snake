package uk.co.bithatch.snake.ui;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import com.sshtools.icongenerator.IconBuilder;
import com.sshtools.icongenerator.IconBuilder.AwesomeIconMode;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import uk.co.bithatch.snake.lib.Macro;
import uk.co.bithatch.snake.lib.MacroKey;
import uk.co.bithatch.snake.lib.MacroSequence;

public class Record extends AbstractDetailsController {

	public enum State {
		IDLE, RECORDING, PAUSED
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

	public void setMacroSequence(MacroSequence seq) {

		empty.managedProperty().bind(empty.visibleProperty());

		handler = new EventHandler<KeyEvent>() {

			@Override
			public void handle(KeyEvent event) {
				System.out.println(event);
			}
		};

		var builder = Macros.builderForMacroSequence(seq);
		builder.width(32);
		builder.height(32);
		var icon = builder.build(Canvas.class);
		headerImageGroup.getChildren().add(icon);

		updateState(State.IDLE);
		rebuildSeq(seq);
	}

	private void rebuildSeq(MacroSequence seq) {
		keys.getChildren().clear();
		for (Macro m : seq) {
			if (m instanceof MacroKey) {
				MacroKey mk = (MacroKey) m;
				if (mk.getPrePause() > 0) {
					IconBuilder builder = new IconBuilder();
					builder.width(80);
					builder.height(24);
					builder.rect();
					builder.color(Integer.parseInt("00ffff", 16));
					builder.textColor(0);
					builder.bold(true);
					builder.text(String.format("%dms", mk.getPrePause()));
					keys.getChildren().add(builder.build(Canvas.class));
				}
			}
			IconBuilder b = Macros.builderForMacro(m);
			b.width(48);
			b.height(48);
			VBox vb = new VBox();
			vb.getStyleClass().add("column");
			if (m instanceof MacroKey) {
				b.icon(null);
				b.awesomeIconMode(AwesomeIconMode.NONE);
				MacroKey mk = (MacroKey) m;
				if (mk.getState() == uk.co.bithatch.snake.lib.MacroKey.State.DOWN)
					b.text(MessageFormat.format(bundle.getString("down"), mk.getKey().name()));
				else
					b.text(MessageFormat.format(bundle.getString("up"), mk.getKey().name()));
			}
			keys.getChildren().add(b.build(Canvas.class));
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
	void evtBack(ActionEvent evt) {
		context.pop();
	}

	@FXML
	void evtStartRecord(ActionEvent evt) {
		updateState(State.RECORDING);
	}

	@FXML
	void evtPause(ActionEvent evt) {
		updateState(State.PAUSED);
	}

	@FXML
	void evtStop(ActionEvent evt) {
		updateState(State.IDLE);
	}

}
