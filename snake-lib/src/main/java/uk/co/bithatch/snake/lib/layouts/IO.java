package uk.co.bithatch.snake.lib.layouts;

public interface IO {
	
	public interface IOListener {
		void elementChanged(IO element);
	}
	
	void addListener(IOListener listener);
	
	void removeListener(IOListener listener);

	String getLabel();

	float getX();

	float getY();

	void setX(float x);

	void setY(float y);

	void setLabel(String label);
}
