package uk.co.bithatch.snake.ui;

import java.util.ResourceBundle;

import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.ui.audio.AudioManager.VolumeListener;

public class VolumeControl extends ControlController implements VolumeListener {

	final static ResourceBundle bundle = ResourceBundle.getBundle(VolumeControl.class.getName());

	@FXML
	private Slider volume;
	@FXML
	private Label muteToggle;

	private boolean adjusting;

	@Override
	protected void onSetControlDevice() {
		context.getAudioManager().addVolumeListener(this);
		volume.valueProperty().addListener((e, o, n) -> {
			try {
				if (!adjusting) {
					context.getAudioManager().setVolume(getDevice(), n.intValue());

					// TODO remove when events are firing
					setVolumeForState();
				}
			} catch (Exception exx) {
				exx.printStackTrace();
			}
		});
		setVolumeForState();
	}

	@FXML
	void evtMuteToggle() {

	}

	void setVolumeForState() {
		adjusting = true;
		try {
			if (context.getAudioManager().isMuted(getDevice())
					|| context.getAudioManager().getVolume(getDevice()) == 0) {
				volume.setValue(0);
				muteToggle.graphicProperty().set(new FontIcon(FontAwesome.VOLUME_OFF));
			} else {
				volume.setValue(context.getAudioManager().getVolume(getDevice()));
				muteToggle.graphicProperty().set(new FontIcon(FontAwesome.VOLUME_DOWN));
			}
		} finally {
			adjusting = false;
		}
	}

	@Override
	public void volumeChanged(Device device, int volume) {
		if (device.equals(getDevice()))
			Platform.runLater(() -> setVolumeForState());
	}
}
