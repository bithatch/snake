package uk.co.bithatch.snake.widgets;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;

public class LEDGraphic extends AbstractGraphic {

	public LEDGraphic() {
		this(22, 22);
	}

	public LEDGraphic(double width, double height) {
		super(width, height);
	}

	@Override
	public void draw() {
		Paint outlineColor = getOutlineColor();
		Paint ledColor = getLedColor();
		if (ledColor == null)
			ledColor = outlineColor;

		GraphicsContext ctx = getGraphicsContext2D();
		double canvasWidth = boundsInLocalProperty().get().getWidth();
		double canvasHeight = boundsInLocalProperty().get().getHeight();
		ctx.clearRect(0, 0, canvasWidth, canvasHeight);

		ctx.setStroke(outlineColor);
		int lineWidth = 2;
		ctx.setLineWidth(lineWidth);
		ctx.strokeOval(lineWidth, lineWidth, canvasWidth - (lineWidth * 2), canvasHeight - (lineWidth * 2));

		ctx.setFill(ledColor);
		ctx.fillOval(canvasWidth * 0.25f, canvasWidth * 0.25f, canvasWidth / 2f, canvasHeight / 2f);
	}
}
