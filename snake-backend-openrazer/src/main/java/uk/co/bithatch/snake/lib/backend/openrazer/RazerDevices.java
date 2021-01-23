package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;

@DBusInterfaceName("razer.devices")
public interface RazerDevices  extends DBusInterface {


	@DBusMemberName("device_added")
	class DeviceAdded extends DBusSignal {

		public DeviceAdded(String path) throws DBusException {
			super(path);
		}
	}

	@DBusMemberName("device_removed")
	class DeviceRemoved extends DBusSignal {

		public DeviceRemoved(String path) throws DBusException {
			super(path);
		}
	}
	void enableTurnOffOnScreensaver(boolean enable);

	String[] getDevices();

	boolean getOffOnScreensaver();

	boolean getSyncEffects();
	
	String supportedDevices();

	void syncEffects(boolean enabled);
}
