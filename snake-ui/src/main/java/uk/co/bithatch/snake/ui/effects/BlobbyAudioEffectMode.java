package uk.co.bithatch.snake.ui.effects;

import uk.co.bithatch.jdraw.RGBCanvas;

public class BlobbyAudioEffectMode implements AudioEffectMode {
	private RGBCanvas canvas;
	private int[] col1;
	private int[] col2;
	private AudioEffectHandler handler;

	@Override
	public Boolean isNeedsFFT() {
		return true;
	}

	public void frame(double[] audio) {
		int width = canvas.getWidth();
		int height = canvas.getHeight();
		int l = audio.length;

		canvas.clear();

		float step = (float) 1 / (float) width;
		canvas.setColour(col2);

		for (float i = 0; i < 1; i += step) {
			int index = (int) (i * (float) l);
			double amp = audio[index];
			int barHeight = (int) Math.round(amp * ((float) width / 2.0));
			if (barHeight > 0) {
				int arcs = (int) Math.round(Math.max(1, (float) barHeight));
				for (int j = 0; j < arcs; j++) {
					canvas.drawArc(0, 1, (int) (1 + j),
							Math.round(Math.toDegrees((Math.PI * 2 / height) * (i / (l / height)))),
							Math.round(Math.toDegrees((Math.PI * 2 / width) * (i / (l / width) + 1) - .05)));
				}
			}
			if (i == 0)
				canvas.setColour(col1);
		}

	}

	@Override
	public void init(RGBCanvas canvas, AudioEffectHandler handler) {
		this.handler = handler;
		this.canvas = canvas;
		update();

	}

	@Override
	public void update() {
		col1 = handler.getColor1();
		col2 = handler.getColor2();
	}

}