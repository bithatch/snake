package uk.co.bithatch.snake.lib.effects;

import java.util.Arrays;

public class Ripple extends Effect {
	public enum Mode {
		RANDOM, SINGLE
	}

	private int[] color = new int[] { 0, 255, 0 };

	private Mode mode = Mode.SINGLE;
	private double refreshRate = 100;

	public Ripple() {
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Ripple other = (Ripple) obj;
		if (!Arrays.equals(color, other.color))
			return false;
		if (mode != other.mode)
			return false;
		if (Double.doubleToLongBits(refreshRate) != Double.doubleToLongBits(other.refreshRate))
			return false;
		return true;
	}

	public int[] getColor() {
		return color;
	}

	public Mode getMode() {
		return mode;
	}

	public double getRefreshRate() {
		return refreshRate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(color);
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
		long temp;
		temp = Double.doubleToLongBits(refreshRate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	public void setColor(int[] color) {
		this.color = color;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public void setRefreshRate(double refreshRate) {
		this.refreshRate = refreshRate;
	}

	@Override
	public String toString() {
		return "Ripple [mode=" + mode + ", color=" + Arrays.toString(color) + ", refreshRate=" + refreshRate + "]";
	}
}
