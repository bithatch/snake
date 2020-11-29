package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.binding.lighting")
public interface RazerBindingLighting extends DBusInterface {

	String getMatrix(String profile, String mapping);

	void setMatrix(String profile, String mapping, String matrix);

	ThreeTuple<Boolean, Boolean, Boolean> getProfileLEDs(String profile, String mapping);

	void setProfileLEDs(String profile, String mapping, boolean red, boolean green, boolean blue);
}
