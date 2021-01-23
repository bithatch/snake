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
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.DeviceType;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.layouts.Accessory;
import uk.co.bithatch.snake.lib.layouts.Accessory.AccessoryType;
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
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;
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

	public List<DeviceLayout> getLayouts() {
		List<DeviceLayout> l = new ArrayList<>();
		for (DeviceLayout a : backend.getLayouts()) {
			Device device = context.getBackend().getDevice(a.getName());
			if (device != null) {
				DeviceLayout u = getUserLayout(device);
				if (u != null)
					l.add(u);
				else
					l.add(a);
			} else
				l.add(a);
		}
		return l;
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
					if (element instanceof MatrixIO && ((MatrixIO) element).isMatrixLED()) {
						elementNode.putInt("matrixX", ((MatrixIO) element).getMatrixX());
						elementNode.putInt("matrixY", ((MatrixIO) element).getMatrixY());
					} else {
						elementNode.remove("matrixX");
						elementNode.remove("matrixY");
					}
					if (element instanceof Key && ((Key) element).getEventCode() != null) {
						elementNode.put("eventCode", ((Key) element).getEventCode().name());
					} else {
						elementNode.remove("eventCode");
					}
					if (element instanceof Key && ((Key) element).getLegacyKey() != null) {
						elementNode.put("legacyKey", ((Key) element).getLegacyKey().name());
					} else {
						elementNode.remove("legacyKey");
					}
					if (element instanceof MatrixCell) {
						if (((MatrixCell) element).getRegion() != null)
							elementNode.put("region", ((MatrixCell) element).getRegion().name());
						else
							elementNode.remove("region");
						elementNode.putBoolean("disabled", ((MatrixCell) element).isDisabled());
						elementNode.putInt("width", ((MatrixCell) element).getWidth());
					}
					if (element instanceof Area) {
						if (((Area) element).getRegion() != null)
							elementNode.put("region", ((Area) element).getRegion().name());
					}
					if (element instanceof Accessory) {
						elementNode.put("accessory", ((Accessory) element).getAccessory().name());
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

		DeviceLayout layout = backend.hasLayout(device) ? backend.getLayout(device) : null;
		if(layout == null || layout.isReadOnly()) {
			/* Either a built-in or an add-on, is there a user layout to override it? */
			DeviceLayout userLayout = getUserLayout(device);
			if (userLayout != null) {
				return userLayout;
			}
		}

		return layout;
	}

	protected DeviceLayout getUserLayout(Device device) {
		String id = Strings.toId(device.getName());
		try {
			if (prefs.nodeExists(id)) {
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
						try {
							ComponentType type = ComponentType
									.valueOf(elementNode.get("type", ComponentType.LED.name()));
							IO element;
							switch (type) {
							case LED:
								LED led = new LED(view);
								setMatrixPositionFromPreferences(elementNode, led);
								String regionName = elementNode.get("region", null);
								element = led;
								break;
							case KEY:
								Key key = new Key(view);
								setMatrixPositionFromPreferences(elementNode, key);
								String name = elementNode.get("eventCode", "");
								key.setEventCode(name.equals("") ? null : EventCode.valueOf(name));
								name = elementNode.get("legacyKey", "");
								key.setLegacyKey(name.equals("") ? null : uk.co.bithatch.snake.lib.Key.valueOf(name));
								regionName = elementNode.get("region", null);
								element = key;
								break;
							case MATRIX_CELL:
								MatrixCell matrixCell = new MatrixCell(view);
								setMatrixPositionFromPreferences(elementNode, matrixCell);
								matrixCell.setDisabled(elementNode.getBoolean("disabled", false));
								matrixCell.setWidth(elementNode.getInt("width", 0));
								regionName = elementNode.get("region", null);
								if (regionName != null)
									matrixCell.setRegion(Region.Name.valueOf(regionName));
								element = matrixCell;
								break;
							case AREA:
								Area area = new Area(view);
								regionName = elementNode.get("region", null);
								if (regionName != null)
									area.setRegion(Region.Name.valueOf(regionName));
								element = area;
								break;
							case ACCESSORY:
								Accessory accessory = new Accessory(view);
								accessory.setAccessory(AccessoryType.valueOf(elementNode.get("accessory", null)));
								element = accessory;
								break;
							default:
								throw new UnsupportedOperationException();
							}

							element.setLabel(elementNode.get("label", null));
							element.setX(elementNode.getFloat("x", 0));
							element.setY(elementNode.getFloat("y", 0));
							view.addElement(element);

						} catch (IllegalArgumentException iae) {
							LOG.log(Level.WARNING, "Failed to load layout element. ", iae);
						}
					}
					dl.addView(view);
				}
				backend.registerLayout(dl);
				return dl;
			}
		} catch (BackingStoreException bse) {
			throw new IllegalStateException(bse);
		}
		return null;
	}

	protected void setMatrixPositionFromPreferences(Preferences elementNode, MatrixIO led) {
		int mx = elementNode.getInt("matrixX", -1);
		try {
			led.setMatrixX(mx);
		} catch (IllegalArgumentException iae) {
			LOG.log(Level.WARNING, String.format("Ignored based Matrix X %d, reset to zero", mx));
			led.setMatrixX(0);
		}
		int my = elementNode.getInt("matrixY", -1);
		try {
			led.setMatrixY(my);
		} catch (IllegalArgumentException iae) {
			LOG.log(Level.WARNING, String.format("Ignored based Matrix Y %d, reset to zero", my));
			led.setMatrixY(0);
		}
		led.setMatrixY(my);
	}

	public void addListener(Listener listener) {
		backend.addListener(listener);
	}

	public void removeListener(Listener listener) {
		backend.removeListener(listener);
	}

	public boolean hasOfficialLayout(Device device) {
		return backend.hasOfficialLayout(device);
	}

	public boolean hasLayout(DeviceLayout layout) {
		return backend.getLayouts().contains(layout);
	}

	public void remove(DeviceLayout layout) {
		if (!layout.isReadOnly()) {
			try {
				prefs.node(Strings.toId(layout.getName())).removeNode();
			} catch (BackingStoreException e) {
				LOG.log(Level.ERROR, "Failed to remove layout node.", e);
			}
		}
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
		context.getSchedulerManager().get(Queue.APP_IO).execute(() -> {
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
