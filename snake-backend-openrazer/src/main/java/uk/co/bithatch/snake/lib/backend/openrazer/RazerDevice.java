package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.DBusMemberName;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;

@DBusInterfaceName("razer.device.misc")
public interface RazerDevice extends DBusInterface {

	String getDeviceImage();

	String getDeviceMode();

	String getDeviceName();

	String getDeviceType();

	String getDriverVersion();

	String getFirmware();

	String getKeyboardLayout();

	int[] getMatrixDimensions();

	int getPollRate();

	String getRazerUrls();

	String getSerial();

	int[] getVidPid();

	boolean hasDedicatedMacroKeys();

	boolean hasMatrix();

	void resumeDevice();

	void setDeviceMode(byte mode, byte param);

	void setPollRate(short rate);

	void suspendDevice();
}
