package uk.co.bithatch.snake.lib.effects;

import java.util.Arrays;

public class Reactive extends Effect {

	private int[] color = new int[] { 0, 255, 0 };
	private int speed = 100;

	public Reactive() {

	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Reactive other = (Reactive) obj;
		if (!Arrays.equals(color, other.color))
			return false;
		if (speed != other.speed)
			return false;
		return true;
	}

	public int[] getColor() {
		return color;
	}

	public int getSpeed() {
		return speed;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(color);
		result = prime * result + speed;
		return result;
	}

	public void setColor(int[] color) {
		this.color = color;
	}

	public void setSpeed(int speed) {
		this.speed = speed;
	}

	@Override
	public String toString() {
		return "Reactive [color=" + Arrays.toString(color) + ", speed=" + speed + "]";
	}
}
