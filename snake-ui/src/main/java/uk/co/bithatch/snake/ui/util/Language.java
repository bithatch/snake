package uk.co.bithatch.snake.ui.util;

public class Language {

	public static boolean[] toBinaryArray(int value) {
		return toBinaryArray(value, -1);
	}

	public static boolean[] toBinaryArray(int value, int length) {
		String bin = Integer.toBinaryString(value);
		if (length != -1) {
			while (bin.length() < length)
				bin = "0" + bin;
		}
		boolean[] arr = new boolean[length == -1 ? bin.length() : length];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = bin.charAt(i) == '1';
		}
		return arr;
	}
}
