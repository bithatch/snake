package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.macro")
public interface RazerMacro extends DBusInterface {
	
	@Deprecated
	void addMacro(String key, String json);

	@Deprecated
	void deleteMacro(String key);

	@Deprecated
	String getMacros();

	@Deprecated
	boolean getModeModifier();

	@Deprecated
	void setModeModifier(boolean modify);
	
	int getMacroKey();
	
	boolean getMacroRecordingState();
	
	void startMacroRecording(String profile, String mapping, int keyCode);
	
	void stopMacroRecording();
}
