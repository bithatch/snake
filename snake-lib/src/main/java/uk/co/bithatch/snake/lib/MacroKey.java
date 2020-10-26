package uk.co.bithatch.snake.lib;

public class MacroKey implements Macro {
	
	public enum State {
		DOWN, UP
	}

	private Key key = Key.M1;
	private long prePause;
	private State state = State.DOWN;

	public Key getKey() {
		return key;
	}

	public long getPrePause() {
		return prePause;
	}

	public State getState() {
		return state;
	}

	public void setKey(Key key) {
		this.key = key;
	}

	public void setPrePause(long prePause) {
		this.prePause = prePause;
	}

	public void setState(State state) {
		this.state = state;
	}

	@Override
	public String toString() {
		return "MacroKey [prePause=" + prePause + ", state=" + state + ", key=" + key + "]";
	}

	@Override
	public void validate()  throws ValidationException {
		if(state == null)
			throw new ValidationException("macroKey.missingState");
		if(key == null)
			throw new ValidationException("macroKey.missingKey");
	}

}
