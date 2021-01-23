package uk.co.bithatch.snake.ui.effects;

import uk.co.bithatch.jdraw.RGBCanvas;

public interface AudioEffectMode {
	void init(RGBCanvas canvas, AudioEffectHandler handler);

	Boolean isNeedsFFT();

	void frame(double[] frame);
	
	void update();
}