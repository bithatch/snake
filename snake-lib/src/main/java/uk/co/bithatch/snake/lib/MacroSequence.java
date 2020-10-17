package uk.co.bithatch.snake.lib;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class MacroSequence extends ArrayList<Macro> {

	private Key key;

	public MacroSequence() {
	}

	public MacroSequence(Key key) {
		setMacroKey(key);
	}

	public Key getMacroKey() {
		return key;
	}

	public void setMacroKey(Key key) {
		this.key = key;
	}

	public void validate() throws ValidationException {
		for (Macro m : this)
			m.validate();
	}
}
