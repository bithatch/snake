package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.lighting.logo")
public interface RazerRegionLogo extends DBusInterface {
	
	boolean getLogoActive();
	
	double getLogoBrightness();
	
	void setLogoActive(boolean active);
	
	void setLogoBreathDual(byte red1, byte green1, byte blue1, byte red2, byte green2, byte blue2);
	
	void setLogoBreathRandom();
	
	void setLogoBreathSingle(byte red1, byte green1, byte blue1);
	
	void setLogoBrightness(double brightness);
	
	void setLogoNone();
	
	void setLogoReactive(byte red1, byte green1, byte blue1, byte speed);
	
	void setLogoSpectrum();
	
	void setLogoStatic(byte red1, byte green1, byte blue1);
	
	void setLogoWave(int direction);
}
