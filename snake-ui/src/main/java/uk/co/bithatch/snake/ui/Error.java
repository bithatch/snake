package uk.co.bithatch.snake.ui;

import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import uk.co.bithatch.snake.lib.BackendException;
import uk.co.bithatch.snake.widgets.Direction;

public class Error extends AbstractDeviceController implements PreferenceChangeListener {
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

		decoratedTools.visibleProperty().set(context.getConfiguration().isDecorated());
		context.getConfiguration().getNode().addPreferenceChangeListener(this);
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

	@Override
	protected void onDeviceCleanUp() {
		context.getConfiguration().getNode().removePreferenceChangeListener(this);
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

	@Override
	public void preferenceChange(PreferenceChangeEvent evt) {
		if (evt.getKey().equals(Configuration.PREF_DECORATED)) {
			decoratedTools.visibleProperty().set(context.getConfiguration().isDecorated());
		}

	}
}
