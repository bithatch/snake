package uk.co.bithatch.snake.lib.effects;

import java.util.Arrays;

public class Static extends Effect {

	private int[] color = new int[] { 0, 255, 0 };

	public Static() {

	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Static other = (Static) obj;
		if (!Arrays.equals(color, other.color))
			return false;
		return true;
	}

	public int[] getColor() {
		return color;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(color);
		return result;
	}

	public void setColor(int[] color) {
		this.color = color;
	}

	@Override
	public String toString() {
		return "Static [color=" + Arrays.toString(color) + "]";
	}

}
