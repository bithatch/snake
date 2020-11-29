package uk.co.bithatch.snake.lib.binding;

import java.util.ArrayList;

import uk.co.bithatch.snake.lib.InputEventCode;
import uk.co.bithatch.snake.lib.ValidationException;

@SuppressWarnings("serial")
public abstract class MapSequence extends ArrayList<MapAction> {

	private InputEventCode key;
	private ProfileMap map;

	public MapSequence(ProfileMap map) {
		this.map = map;
	}

	public MapSequence(ProfileMap map, InputEventCode key) {
		setMacroKey(key);
		this.map = map;
	}

	public ProfileMap getMap() {
		return map;
	}

	public InputEventCode getMacroKey() {
		return key;
	}

	public void setMacroKey(InputEventCode key) {
		this.key = key;
	}

	public abstract <A extends MapAction> A addAction(Class<A> actionType, Object value);

	public void validate() throws ValidationException {
		for (MapAction m : this)
			m.validate();
	}

	public abstract void remove();

	public abstract void commit();

	public abstract void record();

	public abstract boolean isRecording();

	public abstract void stopRecording();

	public InputEventCode getLastInputCode() {
		int idx = size() - 1;
		while (idx > -1) {
			MapAction a = get(idx);
			if (a instanceof KeyMapAction)
				return ((KeyMapAction) a).getPress();
			else if (a instanceof ReleaseMapAction)
				return ((ReleaseMapAction) a).getRelease();
			idx--;
		}
		return InputEventCode.BTN_0;
	}
}
