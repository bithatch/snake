package uk.co.bithatch.snake.lib.backend.openrazer;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.prefs.Preferences;

import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.errors.ServiceUnknown;

import uk.co.bithatch.snake.lib.Backend;
import uk.co.bithatch.snake.lib.BackendException;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.effects.Effect;

public class OpenRazerBackend implements Backend {

	final static System.Logger LOG = System.getLogger(OpenRazerBackend.class.getName());
	final static Preferences PREFS = Preferences.userNodeForPackage(OpenRazerBackend.class);

	final ScheduledExecutorService batteryPoll = Executors.newScheduledThreadPool(1);
	private DBusConnection conn;
	private RazerDaemon daemon;
	private List<Device> deviceList;
	private RazerDevices devices;

	private List<BackendListener> listeners = new ArrayList<>();

	@Override
	public void addListener(BackendListener listener) {
		listeners.add(listener);
	}

	@Override
	public void close() throws Exception {
		LOG.log(Level.DEBUG, "Closing OpenRazer backend.");
		try {
			conn.close();
		} catch (Exception e) {
		} finally {
			LOG.log(Level.DEBUG, "Stopping battery poll thread.");
			batteryPoll.shutdown();
		}
	}

	@Override
	public List<Device> getDevices() throws Exception {
		if (deviceList == null) {
			List<Device> newDeviceList = new ArrayList<>();
			try {
				for (String d : devices.getDevices()) {
					newDeviceList.add(new NativeRazerDevice(d, conn, this));
				}
				Collections.sort(newDeviceList, (c1, c2) -> c1.getSerial().compareTo(c2.getSerial()));
			} catch (ServiceUnknown se) {
				throw new BackendException("OpenRazer not available", se);
			}
			deviceList = newDeviceList;
		}
		return deviceList;
	}

	@Override
	public String getName() {
		return "OpenRazer";
	}

	@Override
	public Set<Class<? extends Effect>> getSupportedEffects() {
		Set<Class<? extends Effect>> l = new LinkedHashSet<>();
		try {
			for (Device d : getDevices()) {
				l.addAll(d.getSupportedEffects());
			}
		} catch (Exception e) {
		}
		return l;
	}

	@Override
	public String getVersion() {
		return daemon.version();
	}

	@Override
	public void init() throws Exception {
		conn = DBusConnection.getConnection(DBusConnection.DBusBusType.SESSION);
		devices = conn.getRemoteObject("org.razer", "/org/razer", RazerDevices.class);
		daemon = conn.getRemoteObject("org.razer", "/org/razer", RazerDaemon.class);
	}

	@Override
	public boolean isGameMode() {
		try {
			for (Device d : getDevices()) {
				if (d.getCapabilities().contains(Capability.GAME_MODE) && d.isGameMode())
					return true;
			}
		} catch (Exception e) {
		}
		return false;
	}

	@Override
	public boolean isSync() {
		return devices.getSyncEffects();
	}

	@Override
	public void removeListener(BackendListener listener) {
		listeners.add(listener);

	}

	@Override
	public void setGameMode(boolean gameMode) {
		try {
			for (Device d : getDevices()) {
				if (d.getCapabilities().contains(Capability.GAME_MODE))
					d.setGameMode(gameMode);
			}
		} catch (Exception e) {
		}
	}

	@Override
	public void setSync(boolean sync) {
		devices.syncEffects(sync);
	}

	ScheduledExecutorService getBatteryPoll() {
		return batteryPoll;
	}
}
