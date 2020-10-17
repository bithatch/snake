package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.led.gamemode")
public interface RazerGameMode extends DBusInterface {
	
	boolean getGameMode();
	
	void setGameMode(boolean gameMode);
	
}
