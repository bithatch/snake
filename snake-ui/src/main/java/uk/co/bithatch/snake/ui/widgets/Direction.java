package uk.co.bithatch.snake.ui.widgets;

public enum Direction {
	FROM_LEFT, FROM_RIGHT, FROM_TOP, FROM_BOTTOM, FADE;

	public Direction opposite() {
		switch (this) {
		case FROM_LEFT:
			return FROM_RIGHT;
		case FROM_RIGHT:
			return FROM_LEFT;
		case FROM_TOP:
			return FROM_BOTTOM;
		case FROM_BOTTOM:
			return FROM_TOP;
		default:
			return FADE;
		}
	}
}