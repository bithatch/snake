package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.lighting.brightness")
public interface RazerBrightness extends DBusInterface {
	
	double getBrightness();
	
	void setBrightness(double brightness);
	
	
}
