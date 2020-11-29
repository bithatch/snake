package uk.co.bithatch.snake.lib.layouts;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractIO implements IO {
	private String label;
	private float x;
	private float y;
	private List<IOListener> listeners = new ArrayList<>();
	private DeviceView view;

	protected AbstractIO() {
		this((DeviceView) null);
	}

	protected AbstractIO(DeviceView view) {
		this.view = view;
	}

	protected AbstractIO(AbstractIO io) {
		this.label = io.label;
		this.x = io.x;
		this.y = io.y;
	}

	@Override
	public DeviceView getView() {
		return view;
	}

	@Override
	public void addListener(IOListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(IOListener listener) {
		listeners.add(listener);
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public void setLabel(String label) {
		this.label = label;
		fireChanged();
	}

	@Override
	public float getX() {
		return x;
	}

	@Override
	public float getY() {
		return y;
	}

	@Override
	public void setX(float x) {
		this.x = x;
		fireChanged();
	}

	@Override
	public void setY(float y) {
		this.y = y;
		fireChanged();
	}

	@Override
	public String toString() {
		return "AbstractIO [label=" + label + ", x=" + x + ", y=" + y + "]";
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	void fireChanged() {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).elementChanged(this);
	}

	public void setView(DeviceView view) {
		this.view = view;
	}

	protected static String toName(String name) {
		if (name == null || name.length() == 0)
			return name;
		return (name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase()).replace('_', ' ');
	}
}
