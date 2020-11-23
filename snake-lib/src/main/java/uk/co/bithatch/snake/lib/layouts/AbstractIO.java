package uk.co.bithatch.snake.lib.layouts;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractIO implements IO {
	private boolean disabled;
	private String label;
	private int width;
	private float x;
	private float y;
	private List<IOListener> listeners = new ArrayList<>();

	public void addListener(IOListener listener) {
		listeners.add(listener);
	}

	public void removeListener(IOListener listener) {
		listeners.add(listener);
	}

	public String getLabel() {
		return label;
	}

	public int getWidth() {
		return width;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
		fireChanged();
	}

	public void setLabel(String label) {
		this.label = label;
		fireChanged();
	}

	public void setWidth(int width) {
		this.width = width;
		fireChanged();
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public void setX(float x) {
		this.x = x;
		fireChanged();
	}

	public void setY(float y) {
		this.y = y;
		fireChanged();
	}

	@Override
	public String toString() {
		return "AbstractIO [disabled=" + disabled + ", label=" + label + ", width=" + width + ", x=" + x + ", y=" + y
				+ "]";
	}

	void fireChanged() {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).elementChanged(this);
	}
}
