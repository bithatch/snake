package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.lighting.left")
public interface RazerRegionLeft extends DBusInterface {
	
	double getLeftBrightness();
	
	void setLeftBreathDual(byte red1, byte green1, byte blue1, byte red2, byte green2, byte blue2);
	
	void setLeftBreathRandom();
	
	void setLeftBreathSingle(byte red1, byte green1, byte blue1);
	
	void setLeftBrightness(double brightness);
	
	void setLeftNone();
	
	void setLeftReactive(byte red1, byte green1, byte blue1, byte speed);
	
	void setLeftSpectrum();
	
	void setLeftStatic(byte red1, byte green1, byte blue1);
	
	void setLeftWave(int direction);
	
	
}
