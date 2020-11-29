package uk.co.bithatch.snake.lib.binding;

import java.util.Map;

import uk.co.bithatch.snake.lib.InputEventCode;
import uk.co.bithatch.snake.lib.effects.Matrix;

public interface ProfileMap {

	void setMatrix(Matrix matrix);

	Matrix getMatrix();

	boolean[] getLEDs();

	void setLEDs(boolean red, boolean green, boolean blue);

	default void setLEDs(boolean[] rgb) {
		setLEDs(rgb[0], rgb[1], rgb[2]);
	}

	void record(int keyCode);

	boolean isActive();

	void activate();

	Profile getProfile();

	String getId();

	Map<InputEventCode, MapSequence> getSequences();

	void remove();

	void stopRecording();

	boolean isRecording();

	int getRecordingMacroKey();

	boolean isDefault();

	void makeDefault();

	MapSequence addSequence(InputEventCode key, boolean addDefault);

	@SuppressWarnings("resource")
	default InputEventCode getNextFreeKey() {
		for (InputEventCode code : getProfile().getDevice().getSupportedInputEvents()) {
			if (!getSequences().containsKey(code))
				return code;
		}
		throw new IllegalStateException("No free supported input codes. Please remove an existing mapping.");
	}


}
