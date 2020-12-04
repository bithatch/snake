package uk.co.bithatch.snake.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.snake.lib.Region.Name;
import uk.co.bithatch.snake.lib.binding.Profile;
import uk.co.bithatch.snake.lib.binding.ProfileMap;
import uk.co.bithatch.snake.lib.layouts.ComponentType;

public interface Device extends AutoCloseable, Grouping, Lit {

	public interface Listener {
		void changed(Device device, Region region);

		void activeMapChanged(ProfileMap map);

		void profileAdded(Profile profile);

		void profileRemoved(Profile profile);

		void mapAdded(ProfileMap profile);

		void mapChanged(ProfileMap profile);

		void mapRemoved(ProfileMap profile);
	}

	Set<EventCode> getSupportedInputEvents();

	Set<Key> getSupportedLegacyKeys();

	void addListener(Listener listener);

	void addMacro(MacroSequence macro);

	void deleteMacro(Key key);

	String exportMacros();

	int getBattery();

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

	Set<Capability> getCapabilities();

	int[] getDPI();

	String getDriverVersion();

	String getFirmware();

	int getIdleTime();

	String getImage();

	String getImageUrl(BrandingImage image);

	String getKeyboardLayout();

	byte getLowBatteryThreshold();

	Map<Key, MacroSequence> getMacros();

	int[] getMatrixSize();

	int getMaxDPI();

	String getMode();

	String getName();

	int getPollRate();

	List<Region> getRegions();

	String getSerial();

	DeviceType getType();

	void importMacros(String macros);

	boolean isCharging();

	boolean isGameMode();

	boolean isModeModifier();

	boolean isSuspended();

	void removeListener(Listener listener);

	void setBrightness(short brightness);

	void setDPI(short x, short y);

	void setGameMode(boolean gameMode);

	void setIdleTime(int idleTime);

	void setLowBatteryThreshold(byte threshold);

	void setModeModifier(boolean modify);

	void setPollRate(int pollRate);

	void setSuspended(boolean suspended);

	default List<Name> getRegionNames() {
		List<Name> l = new ArrayList<>();
		for (Region r : getRegions()) {
			l.add(r.getName());
		}
		return l;
	}

	default List<ComponentType> getSupportedComponentTypes() {
		List<ComponentType> l = new ArrayList<>();
		l.add(ComponentType.AREA);
		if (getCapabilities().contains(Capability.MATRIX))
			l.add(ComponentType.LED);
		if (getCapabilities().contains(Capability.DEDICATED_MACRO_KEYS))
			l.add(ComponentType.KEY);
		if (getCapabilities().contains(Capability.MACRO_PROFILE_LEDS) || getCapabilities().contains(Capability.MACROS))
			l.add(ComponentType.ACCESSORY);
		return l;
	}

	List<Profile> getProfiles();

	Profile getActiveProfile();

	Profile getProfile(String name);

	Profile addProfile(String name);

	default Region getRegion(Name region) {
		for (Region r : getRegions()) {
			if (r.getName().equals(region))
				return r;
		}
		return null;
	}
}
