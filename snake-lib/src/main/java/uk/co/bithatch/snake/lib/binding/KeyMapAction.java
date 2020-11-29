package uk.co.bithatch.snake.lib.binding;

import uk.co.bithatch.snake.lib.InputEventCode;

public interface KeyMapAction extends MapAction {

	InputEventCode getPress();

	void setPress(InputEventCode press);

	default String getValue() {
		return String.valueOf(getPress().getCode());
	}

	@Override
	default Class<? extends MapAction> getActionType() {
		return KeyMapAction.class;
	}

	default void setValue(String value) {
		setPress(InputEventCode.fromCode(Integer.parseInt(value)));
	}
}
