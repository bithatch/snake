package uk.co.bithatch.snake.lib.layouts;

import java.util.Objects;

import uk.co.bithatch.snake.lib.InputEventCode;

public class Key extends AbstractMatrixIO {

	private InputEventCode eventCode;
	private uk.co.bithatch.snake.lib.Key legacyKey;

	public Key() {
		super();
	}

	public Key(DeviceView view) {
		super(view);
	}

	public Key(Key key) {
		super(key);
		eventCode = key.eventCode;
	}

	public uk.co.bithatch.snake.lib.Key getLegacyKey() {
		return legacyKey;
	}

	public void setLegacyKey(uk.co.bithatch.snake.lib.Key legacyKey) {
		if (!Objects.equals(legacyKey, this.legacyKey)) {
			this.legacyKey = legacyKey;
			fireChanged();
		}
	}

	public InputEventCode getEventCode() {
		return eventCode;
	}

	@Override
	public String getDefaultLabel() {
		if (eventCode == null || isMatrixLED())
			return super.getDefaultLabel();
		else {
			if (eventCode.name().startsWith("BTN_")) {
				return toName(eventCode.name().substring(4)) + " Button";
			} else if (eventCode.name().startsWith("KEY_")) {
				return toName(eventCode.name().substring(4)) + " Key";
			} else
				return toName(eventCode.name());
		}
	}

	public void setEventCode(InputEventCode eventCode) {
		if (!Objects.equals(eventCode, this.eventCode)) {
			this.eventCode = eventCode;
			fireChanged();
		}

	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new Key(this);
	}

}
