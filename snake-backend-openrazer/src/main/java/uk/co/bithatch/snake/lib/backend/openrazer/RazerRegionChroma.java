package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

@DBusInterfaceName("razer.device.lighting.chroma")
public interface RazerRegionChroma extends DBusInterface {

	double getBrightness();

	void setBreathDual(byte red1, byte green1, byte blue1, byte red2, byte green2, byte blue2);

	void setBreathRandom();

	void setBreathSingle(byte red1, byte green1, byte blue1);

	void setBrightness(double brightness);

	void setCustom();

	void setKeyRow(byte[] payload);

	void setNone();

	void setReactive(byte red1, byte green1, byte blue1, byte speed);

	void setSpectrum();

	void setStarlightDual(byte red1, byte green1, byte blue1, byte red2, byte green2, byte blue2, byte speed);

	void setStarlightRandom(byte speed);

	void setStarlightSingle(byte red, byte green, byte blue, byte speed);

	void setStatic(byte red1, byte green1, byte blue1);

	void setWave(int direction);

}
