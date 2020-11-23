package uk.co.bithatch.snake.ui;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import javafx.animation.FadeTransition;
import javafx.beans.property.Property;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import uk.co.bithatch.snake.lib.BackendException;
import uk.co.bithatch.snake.ui.widgets.Direction;

public class Error extends AbstractDeviceController {
	final static ResourceBundle bundle = ResourceBundle.getBundle(Error.class.getName());

	@FXML
	private Label errorTitle;
	@FXML
	private Label error;

	@FXML
	private Label errorDescription;
	@FXML
	private HBox decoratedTools;

	@Override
	protected void onConfigure() throws Exception {
		Property<Boolean> decProp = context.getConfiguration().decoratedProperty();
		decoratedTools.visibleProperty().set(decProp.getValue());
		context.getConfiguration().decoratedProperty()
				.addListener((e) -> decoratedTools.visibleProperty().set(decProp.getValue()));
	}

	@FXML
	void evtRetry() {
		try {
			context.push(Overview.class, Direction.FROM_BOTTOM);
			context.remove(this);
		} catch (Exception e) {
			setError(e);
			FadeTransition anim = new FadeTransition(Duration.seconds(1));
			anim.setAutoReverse(true);
			anim.setCycleCount(1);
			anim.setNode(error);
			anim.setFromValue(0.5);
			anim.setToValue(1);
			anim.play();
		}
	}

	public void setError(Throwable e) {
		Throwable rootCause = null;
		while (e != null) {
			rootCause = e;
			if (rootCause instanceof BackendException) {
				errorTitle.textProperty().set(bundle.getString("noBackend"));
				errorDescription.textProperty().set(bundle.getString("noBackend.description"));
				return;
			}
			e = e.getCause();
		}
		errorTitle.textProperty().set(bundle.getString("other"));
		errorDescription.textProperty()
				.set(MessageFormat.format(bundle.getString("other.description"), rootCause.getLocalizedMessage()));
	}

	@FXML
	void evtAbout() {
		context.push(About.class, Direction.FADE);
	}

	@FXML
	void evtOptions() {
		context.push(Options.class, Direction.FROM_BOTTOM);
	}
}
