package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.macro")
public interface RazerMacro extends DBusInterface {
	
	void addMacro(String key, String json);

	void deleteMacro(String key);
	
	String getMacros();
	
	boolean getModeModifier();
	
	void setModeModifier(boolean modify);
	
}
