package uk.co.bithatch.snake.lib.effects;

import java.util.Arrays;

public class Breath extends Effect {

	public enum Mode {
		DUAL, RANDOM, SINGLE
	}

	private int[] color = new int[] { 0, 255, 0 };

	private int[] color1 = new int[] { 0, 255, 0 };
	private int[] color2 = new int[] { 0, 0, 255 };
	private Mode mode = Mode.SINGLE;

	public Breath() {

	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Breath other = (Breath) obj;
		if (!Arrays.equals(color, other.color))
			return false;
		if (!Arrays.equals(color1, other.color1))
			return false;
		if (!Arrays.equals(color2, other.color2))
			return false;
		if (mode != other.mode)
			return false;
		return true;
	}

	public int[] getColor() {
		return color;
	}

	public int[] getColor1() {
		return color1;
	}

	public int[] getColor2() {
		return color2;
	}

	public Mode getMode() {
		return mode;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(color);
		result = prime * result + Arrays.hashCode(color1);
		result = prime * result + Arrays.hashCode(color2);
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
		return result;
	}

	public void setColor(int[] color) {
		this.color = color;
	}

	public void setColor1(int[] color1) {
		this.color1 = color1;
	}

	public void setColor2(int[] color2) {
		this.color2 = color2;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	@Override
	public String toString() {
		return "Breath [mode=" + mode + ", color=" + Arrays.toString(color) + ", color1=" + Arrays.toString(color1)
				+ ", color2=" + Arrays.toString(color2) + "]";
	}

}
