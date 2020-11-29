package uk.co.bithatch.snake.lib.binding;

import uk.co.bithatch.snake.lib.ValidationException;

public interface MapAction {

	void commit();

	MapSequence getSequence();

	ProfileMap getMap();

	int getActionId();

	String getValue();

	void remove();

	void validate() throws ValidationException;

	<A extends MapAction> A update(Class<A> actionType, Object value);

	Class<? extends MapAction> getActionType();

	void setValue(String value);
}
