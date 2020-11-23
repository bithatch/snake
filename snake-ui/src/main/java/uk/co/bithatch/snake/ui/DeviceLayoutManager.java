package uk.co.bithatch.snake.ui;

import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

import javafx.util.Duration;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.DeviceType;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.layouts.Area;
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.DeviceLayout;
import uk.co.bithatch.snake.lib.layouts.DeviceLayouts;
import uk.co.bithatch.snake.lib.layouts.DeviceLayouts.Listener;
import uk.co.bithatch.snake.lib.layouts.DeviceView;
import uk.co.bithatch.snake.lib.layouts.IO;
import uk.co.bithatch.snake.lib.layouts.Key;
import uk.co.bithatch.snake.lib.layouts.LED;
import uk.co.bithatch.snake.lib.layouts.MatrixCell;
import uk.co.bithatch.snake.lib.layouts.MatrixIO;
import uk.co.bithatch.snake.lib.layouts.ViewPosition;
import uk.co.bithatch.snake.ui.util.Prefs;
import uk.co.bithatch.snake.ui.util.Strings;
import uk.co.bithatch.snake.ui.util.Time.Timer;

public class DeviceLayoutManager implements Listener {

	final static System.Logger LOG = System.getLogger(DeviceLayoutManager.class.getName());

	private DeviceLayouts backend;
	private Preferences prefs;
	private Timer saveTimer = new Timer(Duration.seconds(5), (e) -> queueSave());
	private Set<DeviceLayout> dirty = Collections.synchronizedSet(new LinkedHashSet<>());
	private App context;

	public DeviceLayoutManager(App context) {
		this.context = context;
		backend = new DeviceLayouts();
		backend.addListener(this);
		prefs = context.getPreferences().node("layouts");
	}

	public boolean hasLayout(Device device) {
		return hasUserLayout(device) || backend.hasLayout(device);
	}

	public boolean hasUserLayout(Device device) {
		String id = Strings.toId(device.getName());
		try {
			if (prefs.nodeExists(id))
				return true;
		} catch (BackingStoreException e) {
			throw new IllegalStateException("Could not query layouts.", e);
		}
		return false;
	}

	public void addLayout(DeviceLayout layout) {
		backend.addLayout(layout);
	}

	public void save(DeviceLayout layout) {
		if (layout.isReadOnly()) {
			throw new IllegalArgumentException("Cannot save read only layouts.");
		}
		String id = Strings.toId(layout.getName());
		Preferences layoutNode = prefs.node(id);
		layoutNode.put("deviceType", layout.getDeviceType().name());
		layoutNode.putInt("matrixHeight", layout.getMatrixHeight());
		layoutNode.putInt("matrixWidth", layout.getMatrixWidth());
		layoutNode.put("viewOrder", String.join(",",
				layout.getViews().keySet().stream().map(ViewPosition::name).collect(Collectors.toList())));
		Preferences viewsNode = layoutNode.node("views");
		try {
			viewsNode.removeNode();
			viewsNode = layoutNode.node("views");
			for (DeviceView view : layout.getViews().values()) {
				Preferences viewNode = viewsNode.node(view.getPosition().name());
				viewNode.putBoolean("desaturateImage", view.isDesaturateImage());
				viewNode.putFloat("imageOpacity", view.getImageOpacity());
				viewNode.putFloat("imageScale", view.getImageScale());
				if (view.getImageUri() != null)
					viewNode.put("imageUri", view.getImageUri());

				Preferences elementsNode = viewNode.node("elements");
				int idx = 0;
				for (IO element : view.getElements()) {
					Preferences elementNode = elementsNode.node(String.valueOf(idx++));
					elementNode.put("type", ComponentType.fromClass(element.getClass()).name());
					if (element instanceof MatrixIO) {
						elementNode.putInt("matrixX", ((MatrixIO) element).getMatrixX());
						elementNode.putInt("matrixY", ((MatrixIO) element).getMatrixY());
					}
					if (element instanceof MatrixCell) {
						if (((MatrixCell) element).getRegion() != null)
							elementNode.put("region", ((MatrixCell) element).getRegion().name());
					}
					if (element instanceof Area) {
						if (((Area) element).getRegion() != null)
							elementNode.put("region", ((Area) element).getRegion().name());
					}
					elementNode.putFloat("x", element.getX());
					elementNode.putFloat("y", element.getY());
					if (element.getLabel() != null)
						elementNode.put("label", element.getLabel());
				}
			}

			layoutNode.flush();
		} catch (BackingStoreException bse) {
			throw new IllegalStateException("Could not save layout.", bse);
		}
	}

	public DeviceLayout getLayout(Device device) {

		/* Check first if there is a saved layout */
		try {
			String id = Strings.toId(device.getName());
			if (!backend.hasLayout(device) && prefs.nodeExists(id)) {
				Preferences layoutNode = prefs.node(id);
				DeviceLayout dl = new DeviceLayout(device);
				dl.setDeviceType(DeviceType.valueOf(layoutNode.get("deviceType", DeviceType.UNRECOGNISED.name())));
				dl.setMatrixHeight(layoutNode.getInt("matrixHeight", 1));
				dl.setMatrixWidth(layoutNode.getInt("matrixWidth", 1));
				List<String> viewOrder = Arrays.asList(Prefs.getStringList(layoutNode, "viewOrder"));
				Preferences viewsNode = layoutNode.node("views");
				for (String viewPositionName : viewOrder) {
					Preferences viewNode = viewsNode.node(viewPositionName);
					DeviceView view = new DeviceView();
					view.setLayout(dl);
					view.setDesaturateImage(viewNode.getBoolean("desaturateImage", false));
					view.setImageOpacity(viewNode.getFloat("imageOpacity", 1));
					view.setImageScale(viewNode.getFloat("imageScale", 1));
					view.setImageUri(viewNode.get("imageUri", null));
					try {
						view.setPosition(ViewPosition.valueOf(viewPositionName));
					} catch (Exception e) {
						/* Let other views load, we can't really steal anothers position */
						LOG.log(Level.ERROR, "Failed to load view.", e);
						continue;
					}
					Preferences elementsNode = viewNode.node("elements");
					List<String> elementNodeNames = new ArrayList<>(Arrays.asList(elementsNode.childrenNames()));
					Collections.sort(elementNodeNames);
					for (String elementNodeName : elementNodeNames) {
						Preferences elementNode = elementsNode.node(elementNodeName);
						ComponentType type = ComponentType.valueOf(elementNode.get("type", ComponentType.LED.name()));
						IO element;
						switch (type) {
						case LED:
							LED led = new LED();
							led.setMatrixX(elementNode.getInt("matrixX", 0));
							led.setMatrixY(elementNode.getInt("matrixY", 0));
							String regionName = elementNode.get("region", null);
							element = led;
							break;
						case KEY:
							Key key = new Key();
							key.setMatrixX(elementNode.getInt("matrixX", 0));
							key.setMatrixY(elementNode.getInt("matrixY", 0));
							regionName = elementNode.get("region", null);
							element = key;
							break;
						case MATRIX_CELL:
							MatrixCell matrixCell = new MatrixCell();
							matrixCell.setMatrixX(elementNode.getInt("matrixX", 0));
							matrixCell.setMatrixY(elementNode.getInt("matrixY", 0));
							regionName = elementNode.get("region", null);
							if (regionName != null)
								matrixCell.setRegion(Region.Name.valueOf(regionName));
							element = matrixCell;
							break;
						case AREA:
							Area area = new Area();
							regionName = elementNode.get("region", null);
							if (regionName != null)
								area.setRegion(Region.Name.valueOf(regionName));
							element = area;
							break;
						default:
							throw new UnsupportedOperationException();
						}

						element.setLabel(elementNode.get("label", null));
						element.setX(elementNode.getFloat("x", 0));
						element.setY(elementNode.getFloat("y", 0));
						view.addElement(element);
					}
					dl.addView(view);
				}
				backend.addLayout(dl);
				return dl;
			}
		} catch (BackingStoreException bse) {
			throw new IllegalStateException("Could not get layout.", bse);
		}

		return backend.getLayout(device);
	}

	public void addListener(Listener listener) {
		backend.addListener(listener);
	}

	public void removeListener(Listener listener) {
		backend.addListener(listener);
	}

	public boolean hasOfficialLayout(Device device) {
		return backend.hasOfficialLayout(device);
	}

	public boolean hasLayout(DeviceLayout layout) {
		return backend.getLayouts().contains(layout);
	}

	public void remove(DeviceLayout layout) {
		backend.remove(layout);
	}

	@Override
	public void viewChanged(DeviceLayout layout, DeviceView view) {
		changed(layout);
	}

	@Override
	public void viewElementAdded(DeviceLayout layout, DeviceView view, IO element) {
		changed(layout);
	}

	@Override
	public void viewElementChanged(DeviceLayout layout, DeviceView view, IO element) {
		changed(layout);
	}

	@Override
	public void viewElementRemoved(DeviceLayout layout, DeviceView view, IO element) {
		changed(layout);
	}

	@Override
	public void viewAdded(DeviceLayout layout, DeviceView view) {
		changed(layout);
	}

	@Override
	public void viewRemoved(DeviceLayout layout, DeviceView view) {
		changed(layout);
	}

	@Override
	public void layoutChanged(DeviceLayout layout) {
		changed(layout);
	}

	protected void changed(DeviceLayout layout) {
		dirty.add(layout);
		saveTimer.reset();
	}

	void queueSave() {
		context.getLoadQueue().execute(() -> {
			synchronized (dirty) {
				saveAll();
				dirty.clear();
			}
		});
	}

	void saveAll() {
		for (DeviceLayout l : dirty) {
			if (!l.isReadOnly())
				save(l);
		}
	}

	@Override
	public void layoutAdded(DeviceLayout layout) {
		layoutChanged(layout);
	}

	@Override
	public void layoutRemoved(DeviceLayout layout) {
	}
}
