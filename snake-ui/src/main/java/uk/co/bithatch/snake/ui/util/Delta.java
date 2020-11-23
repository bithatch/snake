package uk.co.bithatch.snake.ui.util;

public class Delta {
	private double x, y;

	public Delta() {

	}

	public Delta(double x, double y) {
		super();
		this.x = x;
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public boolean isZero() {
		return x == 0 && y == 0;
	}

	@Override
	public String toString() {
		return "Delta [x=" + x + ", y=" + y + "]";
	}

}