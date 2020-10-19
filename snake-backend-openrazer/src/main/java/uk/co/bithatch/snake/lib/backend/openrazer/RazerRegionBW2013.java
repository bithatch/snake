package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.lighting.bw2013")
public interface RazerRegionBW2013 extends DBusInterface {

	void setPulsate();

	void setStatic();
	
	byte getEffect();

}
