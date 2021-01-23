package uk.co.bithatch.snake.ui.effects;

import uk.co.bithatch.jdraw.Gradient;
import uk.co.bithatch.jdraw.RGBCanvas;

public class ExpandAudioEffectMode implements AudioEffectMode {
	private RGBCanvas canvas;
	private Gradient bg;
	private AudioEffectHandler handler;

	@Override
	public Boolean isNeedsFFT() {
		return true;
	}

	public void frame(double[] audio) {
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		int l = audio.length;

		float freq = (float) audio.length / (float) width;
		int actualCols = (int) ((float) audio.length / freq) + 1;

		canvas.clear();

		canvas.translate((width - actualCols) / 2, 0);
		canvas.setGradient(bg);

		float step = (float) 1 / (float) width;

		for (float i = 0; i < 1; i += step) {
			double amp = audio[(int) (i * (float) l)];
			int barHeight = (int) (amp * height);
			if (barHeight > 0) {
				canvas.fillRect((int) ((i / step)), height / 2 - barHeight / 2, 1, barHeight);
			}
		}

		canvas.translate(0, 0);

	}

	@Override
	public void init(RGBCanvas canvas, AudioEffectHandler handler) {
		this.canvas = canvas;
		this.handler = handler;
		update();

	}

	@Override
	public void update() {
		bg = new Gradient(new float[] { 0f, 0.5f, 1.0f }, handler.getColor1(), handler.getColor2(),
				handler.getColor3());
	}

}