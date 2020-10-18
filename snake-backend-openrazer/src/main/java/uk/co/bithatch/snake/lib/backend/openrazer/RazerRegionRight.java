package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.lighting.right")
public interface RazerRegionRight extends DBusInterface {
	
	double getRightBrightness();
	
	void setRightBreathDual(byte red1, byte green1, byte blue1, byte red2, byte green2, byte blue2);
	
	void setRightBreathRandom();
	
	void setRightBreathSingle(byte red1, byte green1, byte blue1);
	
	void setRightBrightness(double brightness);
	
	void setRightNone();
	
	void setRightReactive(byte red1, byte green1, byte blue1, byte speed);
	
	void setRightSpectrum();
	
	void setRightStatic(byte red1, byte green1, byte blue1);
	
	void setRightWave(int direction);
	
	
}
