package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.lighting.backlight")
public interface RazerRegionBacklight extends DBusInterface {

	boolean getBacklightActive();

	void setBacklightActive(boolean active);
}
