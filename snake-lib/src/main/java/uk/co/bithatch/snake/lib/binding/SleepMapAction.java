package uk.co.bithatch.snake.lib.binding;

public interface SleepMapAction extends MapAction {

	float getSeconds();

	void setSeconds(float seconds);

	default String getValue() {
		return String.valueOf(getSeconds());
	}

	@Override
	default Class<? extends MapAction> getActionType() {
		return SleepMapAction.class;
	}
	
	default void setValue(String value) {
		setSeconds(Float.parseFloat(value));
	}
}
