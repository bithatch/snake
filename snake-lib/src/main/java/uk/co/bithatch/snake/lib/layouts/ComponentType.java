package uk.co.bithatch.snake.lib.layouts;

public enum ComponentType {
	LED, KEY, AREA, MATRIX_CELL;

	public IO createElement() {
		switch (this) {
		case LED:
			return new LED();
		case KEY:
			return new Key();
		default:
			return new Area();
		}
	}

	public static ComponentType fromClass(Class<? extends IO> clazz) {
		if (clazz.equals(uk.co.bithatch.snake.lib.layouts.LED.class)) {
			return LED;
		} else if (clazz.equals(uk.co.bithatch.snake.lib.layouts.Key.class)) {
			return KEY;
		} else if (clazz.equals(uk.co.bithatch.snake.lib.layouts.Area.class)) {
			return AREA;
		} else if (clazz.equals(uk.co.bithatch.snake.lib.layouts.MatrixCell.class)) {
			return MATRIX_CELL;
		} else
			throw new IllegalArgumentException("Unknown type.");
	}
}