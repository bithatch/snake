package uk.co.bithatch.snake.lib.binding;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.EventCode.Ev;

public interface ReleaseMapAction extends MapAction {

	EventCode getRelease();

	void setRelease(EventCode release);

	default String getValue() {
		return String.valueOf(getRelease().code());
	}

	@Override
	default Class<? extends MapAction> getActionType() {
		return ReleaseMapAction.class;
	}

	default void setValue(String value) {
		setRelease(EventCode.fromCode(Ev.EV_KEY, Integer.parseInt(value)));
	}
}
