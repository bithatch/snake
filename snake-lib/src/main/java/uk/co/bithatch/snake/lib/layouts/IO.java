package uk.co.bithatch.snake.lib.layouts;

public interface IO extends Cloneable {

	public interface IOListener {
		void elementChanged(IO element);
	}

	void addListener(IOListener listener);

	void removeListener(IOListener listener);

	String getLabel();

	String getDefaultLabel();

	default String getDisplayLabel() {
		String label = getLabel();
		if (label == null)
			return getDefaultLabel();
		else
			return label;
	}

	float getX();

	float getY();

	void setX(float x);

	void setY(float y);

	void setLabel(String label);

	DeviceView getView();

	Object clone() throws CloneNotSupportedException;
}
