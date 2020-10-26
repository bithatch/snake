package uk.co.bithatch.snake.lib;

public class Colors {

	public static int[] fromHex(String hex) {
		if(hex.startsWith("#"))
			hex = hex.substring(1);
		return new int[] { Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16),
				Integer.parseInt(hex.substring(4, 6), 16) };
	}

	public static String toHex(int[] col) {
		return String.format("#%02x%02x%02x", col[0], col[1], col[2]);
	}
}
