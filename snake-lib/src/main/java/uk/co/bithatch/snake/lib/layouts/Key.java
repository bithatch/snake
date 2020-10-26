package uk.co.bithatch.snake.lib.layouts;

public class Key {

	private boolean disabled;
	private String label;
	private int width;
	private int x;
	private int y;

	public String getLabel() {
		return label;
	}

	public int getWidth() {
		return width;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

}
