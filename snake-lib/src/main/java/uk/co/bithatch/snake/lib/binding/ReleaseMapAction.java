package uk.co.bithatch.snake.lib.binding;

import uk.co.bithatch.snake.lib.InputEventCode;

public interface ReleaseMapAction extends MapAction {

	InputEventCode getRelease();

	void setRelease(InputEventCode release);

	default String getValue() {
		return String.valueOf(getRelease().getCode());
	}

	@Override
	default Class<? extends MapAction> getActionType() {
		return ReleaseMapAction.class;
	}

	default void setValue(String value) {
		setRelease(InputEventCode.fromCode(Integer.parseInt(value)));
	}
}
