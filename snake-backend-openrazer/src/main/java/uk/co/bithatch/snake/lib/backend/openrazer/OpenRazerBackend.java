package uk.co.bithatch.snake.lib.backend.openrazer;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
	private Map<String, Device> deviceMap = new HashMap<>();
	private RazerDevices devices;
	private Object lock = new Object();
	private List<BackendListener> listeners = new ArrayList<>();
	private String[] currentDevices;

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
		synchronized (lock) {
			if (deviceList == null) {
				deviceMap.clear();
				List<Device> newDeviceList = new ArrayList<>();
				try {
					currentDevices = devices.getDevices();
					for (String d : currentDevices) {
						NativeRazerDevice dev = new NativeRazerDevice(d, conn, this);
						deviceMap.put(d, dev);
						newDeviceList.add(dev);
					}
					Collections.sort(newDeviceList, (c1, c2) -> c1.getSerial().compareTo(c2.getSerial()));
				} catch (ServiceUnknown se) {
					throw new BackendException("OpenRazer not available", se);
				}
				deviceList = newDeviceList;
			}
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
		conn.addSigHandler(RazerDevices.DeviceAdded.class, devices, (sig) -> {
			synchronized (lock) {
				List<String> was = Arrays.asList(currentDevices);
				try {
					String[] nowArr = devices.getDevices();
					List<String> now = new ArrayList<>(Arrays.asList(nowArr));
					now.removeAll(was);
					for (String newDevice : now) {
						NativeRazerDevice dev = new NativeRazerDevice(newDevice, conn, this);
						deviceMap.put(newDevice, dev);
						deviceList.add(dev);
						for (int i = listeners.size() - 1; i >= 0; i--) {
							listeners.get(i).deviceAdded(dev);
						}
					}
					currentDevices = nowArr;
				} catch (Exception e) {
					LOG.log(Level.ERROR, "Failed to get newly added device.", e);
				}
			}
		});
		conn.addSigHandler(RazerDevices.DeviceRemoved.class, devices, (sig) -> {
			synchronized (lock) {
				List<String> was = new ArrayList<>(Arrays.asList(currentDevices));
				try {
					String[] nowArr = devices.getDevices();
					List<String> now = Arrays.asList(nowArr);
					was.removeAll(now);
					for (String removedDevice : was) {
						Device dev = deviceMap.get(removedDevice);
						if (dev != null) {
							deviceList.remove(dev);
							deviceMap.remove(removedDevice);
							for (int i = listeners.size() - 1; i >= 0; i--) {
								listeners.get(i).deviceRemoved(dev);
							}
						}
					}
					currentDevices = nowArr;
				} catch (Exception e) {
					LOG.log(Level.ERROR, "Failed to get newly remove device.", e);
				}
			}
		});
		daemon = conn.getRemoteObject("org.razer", "/org/razer", RazerDaemon.class);
		getDevices();

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
