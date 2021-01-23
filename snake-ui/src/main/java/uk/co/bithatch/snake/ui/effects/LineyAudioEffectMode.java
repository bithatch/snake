package uk.co.bithatch.snake.ui.effects;

import uk.co.bithatch.jdraw.RGBCanvas;

public class LineyAudioEffectMode implements AudioEffectMode {
	private RGBCanvas canvas;
	private AudioEffectHandler handler;
	private double[] totData;

	@Override
	public Boolean isNeedsFFT() {
		return true;
	}

	public void frame(double[] audio) {
		double t = 0;
		for (double d : audio)
			t += d;
		t /= (double) audio.length;
		int width = canvas.getWidth();
		totData[width - 1] = Math.min(1, t);
		System.arraycopy(totData, 1, totData, 0, totData.length - 1);
		canvas.clear();
		canvas.setColour(handler.getColor1());
		int height = canvas.getHeight();
		int h = (int) ((float) height / 2.0);
		int y = h;
		for (int j = 0; j < width; j++) {
			if (totData[j] < 0.334) {
				canvas.setColour(handler.getColor1());
			} else if (totData[j] < 0.667) {
				canvas.setColour(handler.getColor2());
			} else {
				canvas.setColour(handler.getColor3());
			}
			int ny = (int) ((double) height * totData[j]);
			if (j == 0)
				canvas.plot(j, height - ny);
			else {
				canvas.drawLine(j - 1, height - y, j, height - ny);
			}
			y = ny;
		}
	}

	@Override
	public void init(RGBCanvas canvas, AudioEffectHandler handler) {
		this.handler = handler;
		this.canvas = canvas;
		totData = new double[canvas.getWidth()];
		update();

	}

	@Override
	public void update() {
	}

}