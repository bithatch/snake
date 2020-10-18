package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.power")
public interface RazerBattery extends DBusInterface {
	
	double getBattery();
	
	boolean isCharging();
	
	void setIdleTime(int idleTime);
	
	void setLowBatteryThreshold(byte lowBatteryThreshold);
	
	
}
