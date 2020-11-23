package uk.co.bithatch.snake.ui.graphics;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;

public class AreaGraphic extends AbstractGraphic {

	public AreaGraphic() {
		this(22, 22);
	}

	public AreaGraphic(double width, double height) {
		super(width, height);
	}

	@Override
	public void draw() {
		Paint outlineColor = getOutlineColor();
		Paint ledColor = getLedColor();
		if(ledColor == null)
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
		ctx.lineTo(halfLine + canvasWidth / 4, halfLine);
		ctx.moveTo(halfLine, halfLine);
		ctx.lineTo(halfLine, canvasWidth / 4 + halfLine);
		
		ctx.moveTo(canvasWidth - 1 - halfLine, halfLine);
		ctx.lineTo(canvasWidth * 0.75f - halfLine, halfLine);
		ctx.moveTo(canvasWidth - 1 - halfLine, halfLine);
		ctx.lineTo(canvasWidth - 1 - halfLine, halfLine + canvasWidth * 0.25f);
		
		ctx.moveTo(halfLine, canvasHeight - 1 - halfLine);
		ctx.lineTo(halfLine + canvasWidth / 4, canvasHeight - 1 - halfLine);
		ctx.moveTo(halfLine, canvasHeight - 1 - halfLine);
		ctx.lineTo(halfLine, canvasWidth * 0.75f - halfLine);
		
		ctx.moveTo(canvasWidth - 1 - halfLine, canvasHeight - 1  - halfLine);
		ctx.lineTo(canvasWidth * 0.75f - halfLine, canvasHeight - 1 - halfLine);
		ctx.moveTo(canvasWidth - 1 - halfLine, canvasHeight - 1  - halfLine);
		ctx.lineTo(canvasWidth - 1 - halfLine, canvasHeight * 0.75f - halfLine);
		
		ctx.stroke();
		ctx.closePath();
		
		ctx.setFill(ledColor);
		ctx.fillRect(canvasWidth * 0.35f, canvasHeight * 0.15f, canvasWidth * 0.25f, canvasWidth * 0.25f);
		ctx.fillOval(canvasWidth * 0.15f, canvasHeight * 0.55f, canvasWidth * 0.25f, canvasWidth * 0.25f);
		ctx.fillPolygon(new double[]{canvasWidth * 0.70f,canvasWidth * 0.80f, canvasWidth * 0.50f},
                new double[]{canvasHeight * 0.45f, canvasHeight * 0.75f, canvasHeight * 0.75f}, 3);
	}
}
