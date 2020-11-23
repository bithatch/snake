package uk.co.bithatch.snake.lib.layouts;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonElement;

import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Json;

public class DeviceLayouts implements uk.co.bithatch.snake.lib.layouts.DeviceLayout.Listener {

	public interface Listener {
		default void layoutChanged(DeviceLayout layout) {
		}

		default void layoutAdded(DeviceLayout layout) {
		}

		default void layoutRemoved(DeviceLayout layout) {
		}

		default void viewChanged(DeviceLayout layout, DeviceView view) {
		}

		default void viewElementAdded(DeviceLayout layout, DeviceView view, IO element) {
		}

		default void viewElementChanged(DeviceLayout layout, DeviceView view, IO element) {
		}

		default void viewElementRemoved(DeviceLayout layout, DeviceView view, IO element) {
		}

		default void viewAdded(DeviceLayout layout, DeviceView view) {
		}

		default void viewRemoved(DeviceLayout layout, DeviceView view) {
		}
	}

	private List<DeviceLayout> layouts = new ArrayList<>();
	private List<Listener> listeners = new ArrayList<>();

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public void addLayout(DeviceLayout layout) {
		if (layouts.contains(layout))
			throw new IllegalStateException("Already contains this layout.");

		DeviceLayout oldLayout = findLayout(layout.getName());
		if (oldLayout != null) {
			remove(layout);
		}
		layouts.add(layout);
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).layoutAdded(layout);
		layout.addListener(this);
	}

	public boolean hasOfficialLayout(Device device) {

		/* Now look for a default resource with the device name */
		URL res = getClass().getResource(device.getName() + ".json");
		if (res == null) {
			return false;
		} else {
			return true;
		}
	}

	public boolean hasLayout(Device device) {

		/* First look for a registered layout for this device */
		for (DeviceLayout layout : layouts) {
			if (layout.getName().equals(device.getName())) {
				return true;
			}
		}

		/* TODO when there are user layouts */
		return hasOfficialLayout(device);
	}

	public DeviceLayout getLayout(Device device) {
		/* First look for a registered layout for this device */
		DeviceLayout layout = findLayout(device.getName());
		if (layout != null)
			return layout;

		/* Now look for a default resource with the device name */
		URL res = getClass().getResource(device.getName() + ".json");
		if (res == null) {
			/* That doesn't exist. Is there a matrix layout we can convert? */
			if (device.getCapabilities().contains(Capability.KEYBOARD_LAYOUT)) {
				DeviceLayout deviceLayout = new DeviceLayout(device);
				deviceLayout.addView(createMatrixView(device));
				addLayout(deviceLayout);
				return deviceLayout;
			} else {
				DeviceLayout deviceLayout = new DeviceLayout(device);
				addLayout(deviceLayout);
				return deviceLayout;
			}
		} else {
			try (InputStream in = res.openStream()) {
				JsonElement json = Json.toJson(in);
				DeviceLayout builtInlayout = new DeviceLayout(Path.of(res.toURI()), json.getAsJsonObject());
				builtInlayout.setReadOnly(true);
				addLayout(builtInlayout);
				return builtInlayout;
			} catch (Exception ioe) {
				throw new IllegalStateException(String.format("Failed to load built-in layout.", res), ioe);
			}
		}
	}

	public static DeviceView createMatrixView(Device device) {
		DeviceView v = new DeviceView();
		int w = device.getMatrixSize()[1];
		int h = device.getMatrixSize()[0];
		Layout legacyLayout = device.getCapabilities().contains(Capability.KEYBOARD_LAYOUT)
				? Layout.get(device.getKeyboardLayout(), w, h)
				: null;
		v.setPosition(ViewPosition.MATRIX);
		if (legacyLayout == null) {
			int number = 1;
			for (int y = 0; y < h; y++) {
				for (int x = 0; x < w; x++) {
					MatrixCell k = new MatrixCell();
					k.setLabel(String.valueOf(number++));
					k.setMatrixX(x);
					k.setMatrixY(y);
					v.addElement(k);
				}
			}
		} else {
			for (MatrixCell[] row : legacyLayout.getKeys()) {
				for (MatrixCell col : row) {
					v.addElement(col);
				}
			}
		}
		return v;
	}

	protected DeviceLayout findLayout(String name) {
		for (DeviceLayout layout : layouts) {
			if (layout.getName().equals(name)) {
				return layout;
			}
		}
		return null;
	}

	public void remove(DeviceLayout layout) {
		if (layouts.remove(layout)) {
			layout.removeListener(this);
			for (int i = listeners.size() - 1; i >= 0; i--)
				listeners.get(i).layoutRemoved(layout);
		}
	}

	public List<DeviceLayout> getLayouts() {
		return Collections.unmodifiableList(layouts);
	}

	@Override
	public void layoutChanged(DeviceLayout layout, DeviceView view) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).layoutChanged(layout);

	}

	@Override
	public void viewRemoved(DeviceLayout layout, DeviceView view) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).viewRemoved(layout, view);
	}

	@Override
	public void viewChanged(DeviceLayout layout, DeviceView view) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).viewChanged(layout, view);
	}

	@Override
	public void viewAdded(DeviceLayout layout, DeviceView view) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).viewAdded(layout, view);
	}

	@Override
	public void viewElementAdded(DeviceLayout layout, DeviceView view, IO element) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).viewElementAdded(layout, view, element);
	}

	@Override
	public void viewElementRemoved(DeviceLayout layout, DeviceView view, IO element) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).viewElementRemoved(layout, view, element);
	}

	@Override
	public void viewElementChanged(DeviceLayout layout, DeviceView view, IO element) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).viewElementChanged(layout, view, element);
	}
}
