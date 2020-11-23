package uk.co.bithatch.snake.ui;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;

public class Confirm extends AbstractController implements Modal {

	@FXML
	private Label title;

	@FXML
	private Label description;

	@FXML
	private Hyperlink yes;

	@FXML
	private Hyperlink no;

	private Runnable onYes;
	private Runnable onNo;

	public void confirm(ResourceBundle bundle, String prefix, Runnable onYes, Object... args) {
		confirm(bundle, prefix, onYes, null, args);
	}

	public void confirm(ResourceBundle bundle, String prefix, Runnable onYes, Runnable onNo, Object... args) {
		title.textProperty().set(MessageFormat.format(bundle.getString(prefix + ".title"), (Object[]) args));
		description.textProperty()
				.set(MessageFormat.format(bundle.getString(prefix + ".description"), (Object[]) args));
		yes.textProperty().set(MessageFormat.format(bundle.getString(prefix + ".yes"), (Object[]) args));
		no.textProperty().set(MessageFormat.format(bundle.getString(prefix + ".no"), (Object[]) args));
		this.onYes = onYes;
		this.onNo = onNo;
	}

	@FXML
	void evtNo() {
		if (onNo != null) {
			onNo.run();
		}
		context.pop();
	}

	@FXML
	void evtYes() {
		onYes.run();
		context.pop();
	}
}
