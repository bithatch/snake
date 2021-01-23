package uk.co.bithatch.snake.ui.drawing;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import uk.co.bithatch.jdraw.Backing;
import uk.co.bithatch.jdraw.RGBCanvas;

public class JavaFXBacking implements Backing {

	private int height;
	private int width;
	private int zoom;
	private GraphicsContext ctx;
	private Canvas canvas;
	private int[] rgb;
	private boolean rot;

	public JavaFXBacking(int width, int height) {

		this.width = width;
		this.height = height;
		if (height > width) {
			if (height > 20)
				zoom = 20;
			else
				zoom = 40;
			canvas = new Canvas(height * zoom, width * zoom);
			rot = true;
		} else {
			if (width > 20)
				zoom = 20;
			else
				zoom = 40;
			canvas = new Canvas(width * zoom, height * zoom);
		}
		ctx = canvas.getGraphicsContext2D();
	}

	public Canvas getCanvas() {
		return canvas;
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

	@Override
	public void plot(int x, int y, int[] rgb) {
		if (this.rgb == null || rgb != this.rgb) {
			this.rgb = rgb;
			ctx.setFill(new Color((float) rgb[0] / 255.0, (float) rgb[1] / 255.0, (float) rgb[2] / 255.0, 1));
		}
		if (rot) {
			if (rgb.equals(RGBCanvas.BLACK)) {
				ctx.clearRect(y * zoom, x * zoom, zoom, zoom);
			} else
				ctx.fillRect(y * zoom, x * zoom, zoom, zoom);
		} else {
			if (rgb.equals(RGBCanvas.BLACK)) {
				ctx.clearRect(x * zoom, y * zoom, zoom, zoom);
			} else
				ctx.fillRect(x * zoom, y * zoom, zoom, zoom);
		}
	}
}
