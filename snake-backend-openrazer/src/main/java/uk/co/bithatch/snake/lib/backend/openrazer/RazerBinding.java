package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.binding")
public interface RazerBinding extends DBusInterface {

	void addAction(String profile, String mapping, int keyCode, String actionType, String value);
	
	void addMap(String profile, String mapping);
	
	void addProfile(String profile);
	
	void clearActions(String profile, String mapping, byte keyCode);
	
	void copyMap(String profile, String mapping, String destProfile, String mapName);
	
	String getActions(String profile, String mapping, String keyCode);
	
	String getActiveMap();
	
	String getActiveProfile();
	
	String getDefaultMap(String profile);
	
	String getMaps(String profile);
	
	String getProfiles();
	
	void removeAction(String profile, String mapping, int keyCode, int actionId);
	
	void removeMap(String profile, String mapping);
	
	void removeProfile(String profile);
	
	void setActiveMap(String mapping);
	
	void setActiveProfile(String profile);
	
	void setDefaultMap(String profile, String mapping);
	
	void updateAction(String profile, String mapping, int keyCode, String actionType, String value, int actionId);
}
