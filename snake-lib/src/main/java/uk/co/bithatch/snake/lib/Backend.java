package uk.co.bithatch.snake.lib;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import uk.co.bithatch.snake.lib.backend.openrazer.OpenRazerBackend;

public interface Backend extends AutoCloseable, Grouping {

	public interface BackendListener {
		void deviceAdded(Device device);

		void deviceRemoved(Device device);
	}

	void init() throws Exception;

	List<Device> getDevices() throws Exception;

	boolean isSync();

	void setSync(boolean sync);

	String getName();

	void addListener(BackendListener tray);

	void removeListener(BackendListener tray);

	default Set<Capability> getCapabilities() {
		Set<Capability> l = new LinkedHashSet<>();
		try {
			for (Device d : getDevices()) {
				l.addAll(d.getCapabilities());
			}
		} catch (Exception e) {
		}
		return l;
	}

	String getVersion();

	static Backend get() {
		return new OpenRazerBackend();
	}

	default int getBattery() {
		int l = 0;
		int tot = 0;
		try {
			for (Device dev : getDevices()) {
				if (dev.getCapabilities().contains(Capability.BATTERY)) {
					l++;
					tot += dev.getBattery();
				}
			}
			return l == 0 ? -1 : (int) ((float) tot / (float) l);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to get battery summary.", e);
		}
	}

	default byte getLowBatteryThreshold() {
		try {
			int t = 0;
			int l = 0;
			for (Device d : getDevices()) {
				if (d.getCapabilities().contains(Capability.BATTERY)) {
					t += d.getLowBatteryThreshold();
					l++;
				}
			}
			return (byte) ((float) t / (float) l);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to get overall brightness.", e);
		}
	}

	default short getBrightness() {
		int tot = 0;
		int l = 0;
		try {
			for (Device d : getDevices()) {
				if (d.getCapabilities().contains(Capability.BRIGHTNESS)) {
					l++;
					tot += d.getBrightness();
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to get overall brightness.", e);
		}
		return (short) ((float) tot / (float) l);
	}

	default void setBrightness(short brightness) {
		try {
			for (Device d : getDevices()) {
				if (d.getCapabilities().contains(Capability.BRIGHTNESS)) {
					d.setBrightness(brightness);
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to get overall brightness.", e);
		}
	}

	default boolean isCharging() {
		try {
			for (Device d : getDevices())
				if (d.getCapabilities().contains(Capability.BATTERY) && d.isCharging())
					return true;
			return false;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to get charging state.", e);
		}
	}
}
