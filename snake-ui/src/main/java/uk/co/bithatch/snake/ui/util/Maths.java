package uk.co.bithatch.snake.ui.util;

public class Maths {

	public static double distance(double startX, double startY, double endX, double endY) {
		return Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
	}
}
