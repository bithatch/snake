package uk.co.bithatch.snake.ui;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class Input extends AbstractController implements Modal {

	public interface Validator {
		boolean validate(Label errorLabel);
	}

	@FXML
	private Hyperlink cancel;

	@FXML
	private Hyperlink confirm;

	@FXML
	private Label description;

	@FXML
	private Label error;

	@FXML
	private TextField input;

	private Runnable onCancel;

	private Runnable onConfirm;
	@FXML
	private Label title;
	private Validator validator;

	public void confirm(ResourceBundle bundle, String prefix, Runnable onConfirm, Runnable onCancel, String... args) {
		title.textProperty().set(MessageFormat.format(bundle.getString(prefix + ".title"), (Object[]) args));
		description.textProperty()
				.set(MessageFormat.format(bundle.getString(prefix + ".description"), (Object[]) args));
		confirm.textProperty().set(MessageFormat.format(bundle.getString(prefix + ".confirm"), (Object[]) args));
		cancel.textProperty().set(MessageFormat.format(bundle.getString(prefix + ".cancel"), (Object[]) args));

		this.onConfirm = onConfirm;
		this.onCancel = onCancel;

		input.requestFocus();
		input.textProperty().addListener((e) -> validate());
		input.onActionProperty().set((e) -> {
			if (!confirm.disabledProperty().get())
				confirm();
		});
	}

	public Hyperlink getCancel() {
		return cancel;
	}

	public Hyperlink getConfirm() {
		return confirm;
	}

	public void confirm(ResourceBundle bundle, String prefix, Runnable onConfirm, String... args) {
		confirm(bundle, prefix, onConfirm, null, args);
	}

	public Validator getValidator() {
		return validator;
	}

	public StringProperty inputProperty() {
		return input.textProperty();
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	protected void validate() {
		if (validator != null) {
			confirm.disableProperty().set(!validator.validate(error));
		}
	}

	void confirm() {
		context.pop();
		onConfirm.run();
	}

	@FXML
	void evtCancel() {
		context.pop();
		if (onCancel != null)
			onCancel.run();
	}

	@FXML
	void evtConfirm() {
		confirm();
	}
}
