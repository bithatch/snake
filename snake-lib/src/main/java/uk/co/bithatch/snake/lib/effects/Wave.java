package uk.co.bithatch.snake.lib.effects;

public class Wave extends Effect {

	public enum Direction {
		BACKWARD, FORWARD
	}

	private Direction direction = Direction.FORWARD;

	public Wave() {
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Wave other = (Wave) obj;
		if (direction != other.direction)
			return false;
		return true;
	}

	public Direction getDirection() {
		return direction;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((direction == null) ? 0 : direction.hashCode());
		return result;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	@Override
	public String toString() {
		return "Wave [direction=" + direction + "]";
	}
}
