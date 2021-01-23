package uk.co.bithatch.snake.ui;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import uk.co.bithatch.snake.lib.effects.Matrix;
import uk.co.bithatch.snake.ui.audio.AudioManager.AudioSource;
import uk.co.bithatch.snake.ui.drawing.JavaFXBacking;
import uk.co.bithatch.snake.ui.effects.AudioEffectHandler;
import uk.co.bithatch.snake.ui.effects.AudioEffectMode;
import uk.co.bithatch.snake.ui.effects.BlobbyAudioEffectMode;
import uk.co.bithatch.snake.ui.effects.BurstyAudioEffectMode;
import uk.co.bithatch.snake.ui.effects.ExpandAudioEffectMode;
import uk.co.bithatch.snake.ui.effects.LineyAudioEffectMode;
import uk.co.bithatch.snake.ui.effects.PeaksAudioEffectMode;
import uk.co.bithatch.snake.widgets.ColorBar;
import uk.co.bithatch.snake.widgets.JavaFX;

public class AudioOptions extends AbstractEffectController<Matrix, AudioEffectHandler> {
	final static ResourceBundle bundle = ResourceBundle.getBundle(AudioOptions.class.getName());

	@FXML
	private ComboBox<AudioSource> source;
	@FXML
	private Slider gain;
	@FXML
	private Spinner<Integer> fps;
	@FXML
	private CheckBox fft;
	@FXML
	private VBox previewContainer;
	@FXML
	private ColorBar color1;
	@FXML
	private ColorBar color2;
	@FXML
	private ColorBar color3;
	@FXML
	private ComboBox<Class<? extends AudioEffectMode>> mode;

	private JavaFXBacking backing;

	@Override
	protected void onConfigure() throws Exception {
		source.getItems().addAll(context.getAudioManager().getSources());
		Configuration cfg = context.getConfiguration();

		/* Global stuff */
		source.getSelectionModel().select(context.getAudioManager().getSource(cfg.getAudioSource()));
		source.valueProperty().addListener((e, o, n) -> cfg.setAudioSource(n.getName()));
		fps.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50));
		fps.getValueFactory().setValue(cfg.getAudioFPS());
		fps.getValueFactory().valueProperty().addListener((e, o, n) -> cfg.setAudioFPS(n));
		fft.selectedProperty().set(cfg.isAudioFFT());
		fft.selectedProperty().addListener((e, o, n) -> cfg.setAudioFFT(n));
		gain.setValue(cfg.getAudioGain());
		gain.valueProperty().addListener((e, o, n) -> cfg.setAudioGain(n.floatValue()));

		if (context.getAudioManager().getError() != null) {
			notifyMessage(MessagePersistence.EVERYTIME, MessageType.DANGER, null,
					MessageFormat.format(bundle.getString("error.noAudioSystem"),
							context.getAudioManager().getError().getLocalizedMessage()),
					60);
		}

	}

	@Override
	protected void onSetEffectHandler() {
		backing = new JavaFXBacking(getEffectHandler().getBacking().getWidth(),
				getEffectHandler().getBacking().getHeight());
		previewContainer.getChildren().add(backing.getCanvas());
		getEffectHandler().getBacking().add(backing);

		mode.getItems().addAll(Arrays.asList(ExpandAudioEffectMode.class, PeaksAudioEffectMode.class,
				BlobbyAudioEffectMode.class, BurstyAudioEffectMode.class, LineyAudioEffectMode.class));
		Callback<ListView<Class<? extends AudioEffectMode>>, ListCell<Class<? extends AudioEffectMode>>> factory = listView -> {
			return new ListCell<Class<? extends AudioEffectMode>>() {
				@Override
				protected void updateItem(Class<? extends AudioEffectMode> item, boolean empty) {
					super.updateItem(item, empty);
					if (item == null || empty) {
						setText("");
					} else {
						setText(bundle.getString("audioEffectMode." + item.getSimpleName()));
					}
				}
			};
		};
		mode.setButtonCell(factory.call(null));
		mode.setCellFactory(factory);

		color1.setColor(JavaFX.toColor(getEffectHandler().getColor1()));
		color1.colorProperty().addListener((e,o,n) -> getEffectHandler().store(getDevice(), this));
		color2.setColor(JavaFX.toColor(getEffectHandler().getColor2()));
		color2.colorProperty().addListener((e,o,n) -> getEffectHandler().store(getDevice(), this));
		color3.setColor(JavaFX.toColor(getEffectHandler().getColor3()));
		color3.colorProperty().addListener((e,o,n) -> getEffectHandler().store(getDevice(), this));
		mode.getSelectionModel().select(getEffectHandler().getMode());
	}

	public AudioSource getSource() {
		return source.getSelectionModel().getSelectedItem();
	}

	@Override
	protected void onDeviceCleanUp() {
		getEffectHandler().getBacking().remove(backing);
	}

	@FXML
	void evtBack() {
		context.pop();
	}

	@FXML
	void evtColor1() {
		getEffectHandler().store(getDevice(), this);
	}

	@FXML
	void evtColor2() {
		getEffectHandler().store(getDevice(), this);
	}

	@FXML
	void evtColor3() {
		getEffectHandler().store(getDevice(), this);
	}

	@FXML
	void evtMode() {
		getEffectHandler().store(getDevice(), this);
	}

	public Class<? extends AudioEffectMode> getMode() {
		return mode.getValue();
	}

	public int[] getColor1() {
		return JavaFX.toRGB(color1.getColor());
	}

	public int[] getColor2() {
		return JavaFX.toRGB(color2.getColor());
	}

	public int[] getColor3() {
		return JavaFX.toRGB(color3.getColor());
	}

}
