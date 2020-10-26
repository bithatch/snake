package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;

@DBusInterfaceName("razer.devices")
public interface RazerDevices  extends DBusInterface {
	public class device_added extends DBusSignal {

		public device_added(String path) throws DBusException {
			super(path);
		}

	}

	public class device_removed extends DBusSignal {

		public device_removed(String path) throws DBusException {
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
