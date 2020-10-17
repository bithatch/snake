package uk.co.bithatch.snake.ui;

import javafx.scene.control.ButtonBase;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;

public class UIHelpers {

	public static void sizeToImage(ButtonBase button, double width, double height) {
		int sz = (int) width;
		int df = sz / 8;
		sz -= df;
		if (button.getGraphic() != null) {
			ImageView iv = ((ImageView) button.getGraphic());
			iv.setFitWidth(width - df);
			iv.setFitHeight(height - df);
			iv.setSmooth(true);
		} else {
			int fs = (int) (sz * 0.6f);
			button.setStyle("-fx-font-size: " + fs + "px;");
		}
		button.setMinSize(width, height);
		button.setMaxSize(width, height);
		button.setPrefSize(width, height);
		button.layout();
	}

	public static String toHex(Color color) {
		return toHex(color, false);
	}

	public static String toHex(Color color, boolean opacity) {
		return toHex(color, opacity ? color.getOpacity() : -1);
	}

	public static String toHex(Color color, double opacity) {
		if (opacity > -1)
			return String.format("#%02x%02x%02x%02x", (int) (color.getRed() * 255), (int) (color.getGreen() * 255),
					(int) (color.getBlue() * 255), (int) (opacity * 255));
		else
			return String.format("#%02x%02x%02x", (int) (color.getRed() * 255), (int) (color.getGreen() * 255),
					(int) (color.getBlue() * 255));
	}

	public static Color toColor(int[] color) {
		return new Color((float) color[0] / 255f, (float) color[1] / 255f, (float) color[2] / 255f, 1);
	}

	public static int[] toRGB(Color color) {
		return new int[] { (int) (color.getRed() * 255.0), (int) (color.getGreen() * 255.0),
				(int) (color.getBlue() * 255.0) };
	}

	public static String toHex(int[] rgb) {
		return String.format("#%02x%02x%02x", rgb[0], rgb[1], rgb[2]);
	}

}
