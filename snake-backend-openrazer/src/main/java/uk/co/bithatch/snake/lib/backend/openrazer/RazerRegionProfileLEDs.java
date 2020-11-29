package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.lighting.profile_led")
public interface RazerRegionProfileLEDs extends DBusInterface {

	boolean getRedLED();

	void setRedLED(boolean red);

	boolean getGreenLED();

	void setGreenLED(boolean green);

	boolean getBlueLED();

	void setBlueLED(boolean blue);

}
