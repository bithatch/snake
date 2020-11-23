package uk.co.bithatch.snake.lib.layouts;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.co.bithatch.snake.lib.Region.Name;
import uk.co.bithatch.snake.lib.layouts.IO.IOListener;

public class DeviceView implements IOListener {

	public interface Listener {
		void viewChanged(DeviceView view);

		default void elementAdded(DeviceView view, IO element) {
		}

		default void elementChanged(DeviceView view, IO element) {
		}

		default void elementRemoved(DeviceView view, IO element) {
		}
	}

	private List<Listener> listeners = new ArrayList<>();
	private List<IO> elements = new ArrayList<>();
	private Map<ComponentType, Map<Cell, IO>> elementMap = new HashMap<>();
	private String imageUri;
	private ViewPosition position = ViewPosition.TOP;
	private boolean desaturateImage = false;
	private float imageScale = 1;
	private float imageOpacity = 1;
	private DeviceLayout layout;

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public void removeListener(Listener listener) {
		listeners.add(listener);
	}

	public DeviceLayout getLayout() {
		return layout;
	}

	public void setLayout(DeviceLayout layout) {
		this.layout = layout;
	}

	public List<IO> getElements() {
		return Collections.unmodifiableList(elements);
	}

	public float getImageOpacity() {
		return imageOpacity;
	}

	public void setImageOpacity(float imageOpacity) {
		this.imageOpacity = imageOpacity;
		fireChanged();
	}

	public Cell getNextFreeCell(ComponentType type) {
		int y = 0;
		int x = 0;
		Cell cellPosition = new Cell(x, y);
		Map<Cell, IO> elementMap = getMapForType(type);
		while (elementMap.containsKey(cellPosition)) {
			x++;
			if (x == layout.getMatrixWidth()) {
				x = 0;
				y++;
				if (y == layout.getMatrixHeight()) {
					y = x = -1;
					break;
				}
			}
			cellPosition = new Cell(x, y);
		}
		if (x == -1)
			/* No more free spaces, will have to give up and leave to user to sort out */
			throw new IllegalStateException("No more free cells.");

		return cellPosition;
	}

	public void removeElement(IO element) {
		if (elements.contains(element)) {
			element.removeListener(this);
			elements.remove(element);
			if (element instanceof MatrixIO) {
				getMapForType(element)
						.remove(new Cell(((MatrixIO) element).getMatrixX(), ((MatrixIO) element).getMatrixY()));
			}
			fireElementRemoved(element);
		}
	}

	public void addElement(IO element) {
		if (!elements.contains(element)) {
			element.addListener(this);
			elements.add(element);
			if (element instanceof MatrixIO) {
				getMapForType(element)
						.put(new Cell(((MatrixIO) element).getMatrixX(), ((MatrixIO) element).getMatrixY()), element);
			}
			fireElementAdded(element);
		}
	}

	private Map<Cell, IO> getMapForType(IO element) {
		ComponentType ct = ComponentType.fromClass(element.getClass());
		return getMapForType(ct);
	}

	protected Map<Cell, IO> getMapForType(ComponentType ct) {
		Map<Cell, IO> m = elementMap.get(ct);
		if (m == null) {
			m = new HashMap<>();
			elementMap.put(ct, m);
		}
		return m;
	}

	public String getImageUri() {
		return imageUri;
	}

	public void setImageUri(String imageUri) {
		this.imageUri = imageUri;
		fireChanged();
	}

	public ViewPosition getPosition() {
		return position;
	}

	public void setPosition(ViewPosition position) {
		this.position = position;
		if(layout != null)
			layout.updatePosition(this);
		fireChanged();
	}

	public boolean isDesaturateImage() {
		return desaturateImage;
	}

	public void setDesaturateImage(boolean desaturateImage) {
		this.desaturateImage = desaturateImage;
		fireChanged();
	}

	public float getImageScale() {
		return imageScale;
	}

	public void setImageScale(float imageScale) {
		this.imageScale = imageScale;
		fireChanged();
	}

	@SuppressWarnings("unchecked")
	public <O extends IO> O getElement(ComponentType type, int matrixX, int matrixY) {
		return (O) getMapForType(type).get(new Cell(matrixX, matrixY));
	}

	void fireChanged() {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).viewChanged(this);
	}

	void fireElementRemoved(IO element) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).elementRemoved(this, element);
	}

	void fireElementChanged(IO element) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).elementChanged(this, element);
	}

	void fireElementAdded(IO element) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).elementAdded(this, element);
	}

	public String getResolvedImageUri(Path base) {
		if (imageUri == null)
			return null;
		try {
			new URL(imageUri);
		} catch (MalformedURLException murle) {
			if (base != null) {
				try {
					return base.resolve(imageUri).toUri().toURL().toExternalForm();
				} catch (MalformedURLException e) {
				}
			} else {
				if (imageUri.startsWith("/")) {
					return new File(imageUri).toURI().toString();
				}
			}
		}

		return imageUri;
	}

	@Override
	public String toString() {
		return "DeviceView [elements=" + elements + ", imageUri=" + imageUri + ", position=" + position
				+ ", desaturateImage=" + desaturateImage + ", imageScale=" + imageScale + ", imageOpacity="
				+ imageOpacity + "]";
	}

	@Override
	public void elementChanged(IO element) {
		fireElementChanged(element);
	}

	public IO getAreaElement(Name name) {
		for (IO el : elements) {
			if (el instanceof Area && ((Area) el).getRegion().equals(name))
				return el;
		}
		return null;
	}
}
