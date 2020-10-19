package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.lighting.custom")
public interface RazerCustom extends DBusInterface {

	void setRipple(byte red1, byte green1, byte blue1, double refreshRate);

	void setRippleRandomColour(double refreshRate);
}
