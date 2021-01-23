package uk.co.bithatch.snake.ui.widgets;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import uk.co.bithatch.snake.lib.binding.ProfileMap;
import uk.co.bithatch.snake.widgets.ProfileLEDs;

public class MapProfileLEDHelper implements Closeable, ChangeListener<boolean[]> {

	private ProfileMap map;
	private ProfileLEDs profileLEDs;

	public MapProfileLEDHelper(ProfileLEDs profileLEDs) {
		this.profileLEDs = profileLEDs;
	}

	public void setMap(ProfileMap map) {
		if (!Objects.equals(map, this.map)) {
			this.map = map;
			updateState();
		}
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public void changed(ObservableValue<? extends boolean[]> observable, boolean[] oldValue, boolean[] newValue) {
		if (map != null) {
			map.setLEDs(newValue);
		}
	}

	protected void updateState() {
		profileLEDs.setRgbs(map.getLEDs());
	}
}
