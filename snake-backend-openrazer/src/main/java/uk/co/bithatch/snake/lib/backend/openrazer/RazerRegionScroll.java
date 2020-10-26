package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.lighting.scroll")
public interface RazerRegionScroll extends DBusInterface {
	
	boolean getScrollActive();
	
	double getScrollBrightness();
	
	void setScrollActive(boolean active);
	
	void setScrollBreathDual(byte red1, byte green1, byte blue1, byte red2, byte green2, byte blue2);
	
	void setScrollBreathRandom();
	
	void setScrollBreathSingle(byte red1, byte green1, byte blue1);
	
	void setScrollBrightness(double brightness);
	
	void setScrollNone();
	
	void setScrollReactive(byte red1, byte green1, byte blue1, byte speed);
	
	void setScrollSpectrum();
	
	void setScrollStatic(byte red1, byte green1, byte blue1);
	
	void setScrollWave(int direction);
	
	
}
