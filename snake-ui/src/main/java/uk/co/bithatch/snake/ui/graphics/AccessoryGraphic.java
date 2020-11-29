package uk.co.bithatch.snake.ui.graphics;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;

public class AccessoryGraphic extends AbstractGraphic {

	public AccessoryGraphic() {
		this(22, 22);
	}

	public AccessoryGraphic(double width, double height) {
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
		double lineWidth = 2;
		double halfLine = lineWidth / 2f;
		ctx.setLineWidth(lineWidth);

		ctx.beginPath();

		ctx.moveTo(halfLine, halfLine);
		ctx.lineTo( canvasWidth - halfLine, halfLine);
		ctx.lineTo(canvasWidth / 2, canvasHeight - halfLine);
		ctx.lineTo(halfLine, halfLine);
		ctx.stroke();
		ctx.closePath();


		ctx.beginPath();
		ctx.moveTo(halfLine * 6, halfLine * 4);
		ctx.lineTo( canvasWidth - ( halfLine * 6 ), halfLine * 4);
		ctx.lineTo(canvasWidth / 2, canvasHeight - ( halfLine * 8 ) );
		ctx.lineTo(halfLine * 6, halfLine * 4);
		ctx.setFill(ledColor);
		ctx.fill();
		ctx.closePath();
	}
}
