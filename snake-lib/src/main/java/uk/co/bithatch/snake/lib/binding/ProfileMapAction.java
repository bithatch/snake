package uk.co.bithatch.snake.lib.binding;

public interface ProfileMapAction extends MapAction {

	String getProfileName();
	
	void setProfileName(String profileName);

	default String getValue() {
		return getProfileName();
	}

	@Override
	default Class<? extends MapAction> getActionType() {
		return ProfileMapAction.class;
	}
	
	default void setValue(String value) {
		setProfileName(value);
	}
}
