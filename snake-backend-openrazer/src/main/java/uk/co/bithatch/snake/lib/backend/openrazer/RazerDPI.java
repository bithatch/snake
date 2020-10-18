package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.dpi")
public interface RazerDPI extends DBusInterface {
	
	int[] getDPI();
	
	int maxDPI();
	
	void setDPI(short x, short y);
	
	
}
