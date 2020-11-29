package uk.co.bithatch.snake.lib.binding;

import java.util.List;

import uk.co.bithatch.snake.lib.Device;

public interface Profile {

	public interface Listener {
		void changed(Profile profile);

		void activeMapChanged(ProfileMap map);

		void mapAdded(ProfileMap profile);

		void mapRemoved(ProfileMap profile);

		void mapChanged(ProfileMap profile);
	}
	
	Device getDevice();

	void addListener(Listener listener);

	void removeListener(Listener listener);

	void activate();

	boolean isActive();

	String getName();

	ProfileMap getDefaultMap();

	void setDefaultMap(ProfileMap map);

	ProfileMap getActiveMap();

	ProfileMap getMap(String id);

	List<ProfileMap> getMaps();

	ProfileMap addMap(String id);

	void remove();

}
