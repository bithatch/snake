package uk.co.bithatch.snake.lib;

import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.co.bithatch.snake.lib.effects.Effect;

public interface Device extends AutoCloseable, Grouping, Lit {

	public interface Listener {
		void changed(Device device, Region region);
	}

	Set<Capability> getCapabilities();

	void addListener(Listener listener);

	void removeListener(Listener listener);

	String getImage();

	DeviceType getType();

	String getKeyboardLayout();

	String getMode();

	String exportMacros();

	void importMacros(String macros);

	String getName();

	String getDriverVersion();

	String getFirmware();

	int getPollRate();

	String getSerial();

	void setPollRate(int pollRate);

	void setSuspended(boolean suspended);

	boolean isSuspended();

	int[] getMatrixSize();

	String getImageUrl(BrandingImage image);

	List<Region> getRegions();

	int[] getDPI();

	int getBattery();

	boolean isCharging();

	void setIdleTime(int idleTime);

	int getIdleTime();

	void setLowBatteryThreshold(byte threshold);

	byte getLowBatteryThreshold();

	int getMaxDPI();

	void setDPI(short x, short y);

	void setBrightness(short brightness);

	boolean isGameMode();

	void setGameMode(boolean gameMode);

	Map<Key, MacroSequence> getMacros();

	void addMacro(MacroSequence macro);

	void deleteMacro(Key key);

	boolean isModeModifier();

	void setModeModifier(boolean modify);

	void setEffect(Effect effect);

	Effect getEffect();

	default short getBrightness() {
		double tot = 0;
		int of = 0;
		for (Region r : getRegions()) {
			if (r.getCapabilities().contains(Capability.BRIGHTNESS_PER_REGION)) {
				tot += r.getBrightness();
				of++;
			}
		}
		return (short) (tot / (double) of);
	}
}
