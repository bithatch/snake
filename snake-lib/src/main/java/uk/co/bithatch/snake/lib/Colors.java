package uk.co.bithatch.snake.lib;

public class Colors {

	public final static int[] COLOR_BLACK = new int[3];

	public static int[] fromHex(String hex) {
		if (hex.startsWith("#"))
			hex = hex.substring(1);
		return new int[] { Integer.parseInt(hex.substring(0, 2), 16), Integer.parseInt(hex.substring(2, 4), 16),
				Integer.parseInt(hex.substring(4, 6), 16) };
	}

	public static String toHex(int[] col) {
		return String.format("#%02x%02x%02x", col[0], col[1], col[2]);
	}

	public static int[] getInterpolated(int[] from, int[] to, float frac) {
		return new int[] { (int) ((to[0] - from[0]) * frac) + from[0], (int) ((to[1] - from[1]) * frac) + from[1],
				(int) ((to[2] - from[2]) * frac) + from[2] };
	}

	public static int[] toIntHSB(int[] rgb) {
		float[] f = toHSB(rgb);
		return new int[] { (int) (f[0] * 255.0), (int) (f[1] * 255.0), (int) (f[2] * 255.0) };
	}

	public static int[] toRGB(int[] hsb) {
		return toRGB((float) hsb[0] / 255.0f, (float) hsb[1] / 255.0f, (float) hsb[2] / 255.0f);
	}

	public static int[] toRGB(float[] hsb) {
		float hue = hsb[0];
		float saturation = hsb[1];
		float brightness = hsb[2];
		return toRGB(hue, saturation, brightness);
	}

	public static int[] toRGB(float hue, float saturation, float brightness) {
		int r = 0, g = 0, b = 0;
		if (saturation == 0) {
			r = g = b = (int) (brightness * 255.0f + 0.5f);
		} else {
			float h = (hue - (float) Math.floor(hue)) * 6.0f;
			float f = h - (float) java.lang.Math.floor(h);
			float p = brightness * (1.0f - saturation);
			float q = brightness * (1.0f - saturation * f);
			float t = brightness * (1.0f - (saturation * (1.0f - f)));
			switch ((int) h) {
			case 0:
				r = (int) (brightness * 255.0f + 0.5f);
				g = (int) (t * 255.0f + 0.5f);
				b = (int) (p * 255.0f + 0.5f);
				break;
			case 1:
				r = (int) (q * 255.0f + 0.5f);
				g = (int) (brightness * 255.0f + 0.5f);
				b = (int) (p * 255.0f + 0.5f);
				break;
			case 2:
				r = (int) (p * 255.0f + 0.5f);
				g = (int) (brightness * 255.0f + 0.5f);
				b = (int) (t * 255.0f + 0.5f);
				break;
			case 3:
				r = (int) (p * 255.0f + 0.5f);
				g = (int) (q * 255.0f + 0.5f);
				b = (int) (brightness * 255.0f + 0.5f);
				break;
			case 4:
				r = (int) (t * 255.0f + 0.5f);
				g = (int) (p * 255.0f + 0.5f);
				b = (int) (brightness * 255.0f + 0.5f);
				break;
			case 5:
				r = (int) (brightness * 255.0f + 0.5f);
				g = (int) (p * 255.0f + 0.5f);
				b = (int) (q * 255.0f + 0.5f);
				break;
			}
		}
		return new int[] { r, g, b };
	}

	public static float[] toHSB(int[] rgb) {
		int r = rgb[0];
		int g = rgb[1];
		int b = rgb[2];
		float hue, saturation, brightness;
		float[] hsbvals = new float[3];
		int cmax = (r > g) ? r : g;
		if (b > cmax)
			cmax = b;
		int cmin = (r < g) ? r : g;
		if (b < cmin)
			cmin = b;

		brightness = ((float) cmax) / 255.0f;
		if (cmax != 0)
			saturation = ((float) (cmax - cmin)) / ((float) cmax);
		else
			saturation = 0;
		if (saturation == 0)
			hue = 0;
		else {
			float redc = ((float) (cmax - r)) / ((float) (cmax - cmin));
			float greenc = ((float) (cmax - g)) / ((float) (cmax - cmin));
			float bluec = ((float) (cmax - b)) / ((float) (cmax - cmin));
			if (r == cmax)
				hue = bluec - greenc;
			else if (g == cmax)
				hue = 2.0f + redc - bluec;
			else
				hue = 4.0f + greenc - redc;
			hue = hue / 6.0f;
			if (hue < 0)
				hue = hue + 1.0f;
		}
		hsbvals[0] = hue;
		hsbvals[1] = saturation;
		hsbvals[2] = brightness;
		return hsbvals;
	}
}
