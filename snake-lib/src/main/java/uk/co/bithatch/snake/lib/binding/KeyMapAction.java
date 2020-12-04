package uk.co.bithatch.snake.lib.binding;

import uk.co.bithatch.linuxio.EventCode;

public interface KeyMapAction extends MapAction {

	EventCode getPress();

	void setPress(EventCode press);

	default String getValue() {
		return String.valueOf(getPress().code());
	}

	@Override
	default Class<? extends MapAction> getActionType() {
		return KeyMapAction.class;
	}

	default void setValue(String value) {
		setPress(EventCode.fromCode(EventCode.Ev.EV_KEY, Integer.parseInt(value)));
	}
}
