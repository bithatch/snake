package uk.co.bithatch.snake.ui.effects;

import uk.co.bithatch.jdraw.Gradient;
import uk.co.bithatch.jdraw.RGBCanvas;

public class PeaksAudioEffectMode implements AudioEffectMode {
	private double[] peakHeights;
	private double[] peakAcceleration;
	private RGBCanvas canvas;
	private AudioEffectHandler handler;
	private Gradient bg;

	@Override
	public Boolean isNeedsFFT() {
		return null;
	}

	public void frame(double[] audio) {
		float freq = (float) audio.length / (float) canvas.getWidth();
		int actualCols = (int) ((float) audio.length / freq) + 1;

		canvas.clear();
		canvas.translate((canvas.getWidth() - actualCols) / 2, 0);

		for (int i = 0; i < audio.length; i += freq) {

			int col = (int) ((float) i / freq);
			int rows = (int) (audio[i] * (double) (canvas.getHeight()));
			canvas.setGradient(bg);

			if (rows > peakHeights[i]) {
				peakHeights[i] = rows;
				peakAcceleration[i] = 0.0;
			} else {
				peakAcceleration[i] += 0.1f;
				peakHeights[i] -= peakAcceleration[i];
			}

			if (peakHeights[i] < 0)
				peakHeights[i] = 0;

			if(rows > 0)
				canvas.fillRect(col, canvas.getHeight() - rows, 1, rows);
			canvas.setGradient(null);
			canvas.setColour(handler.getColor3());
			canvas.fillRect(col, (int)Math.round(canvas.getHeight() - peakHeights[i]), 1, 1);
		}
		canvas.translate(0, 0);
	}

	@Override
	public void init(RGBCanvas canvas, AudioEffectHandler handler) {

		peakHeights = new double[256];
		peakAcceleration = new double[256];

		this.handler = handler;
		this.canvas = canvas;
		
		update();

	}

	@Override
	public void update() {
		bg = new Gradient(new float[] { 0f, 1.0f }, handler.getColor1(), handler.getColor2());
	}

}