package uk.co.bithatch.snake.lib.effects;

import java.util.prefs.Preferences;

public class Wave extends Effect {

	public enum Direction {
		BACKWARD, FORWARD
	}

	private Direction direction = Direction.FORWARD;

	public Wave() {
	}

	public Direction getDirection() {
		return direction;
	}

	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((direction == null) ? 0 : direction.hashCode());
		return result;
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

	@Override
	public String toString() {
		return "Wave [direction=" + direction + "]";
	}

	@Override
	protected void onSave(Preferences prefs) {
		prefs.put("direction", direction.name());
	}

	@Override
	protected void onLoad(Preferences prefs) {
		direction = Direction.valueOf(prefs.get("direction", Direction.FORWARD.name()));
	}
}
