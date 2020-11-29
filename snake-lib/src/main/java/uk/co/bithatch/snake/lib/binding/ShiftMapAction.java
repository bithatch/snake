package uk.co.bithatch.snake.lib.binding;

public interface ShiftMapAction extends MapAction {

	String getMapName();

	void setMapName(String mapName);

	default String getValue() {
		return getMapName();
	}

	@Override
	default Class<? extends MapAction> getActionType() {
		return ShiftMapAction.class;
	}
	
	default void setValue(String value) {
		setMapName(value);
	}
}
