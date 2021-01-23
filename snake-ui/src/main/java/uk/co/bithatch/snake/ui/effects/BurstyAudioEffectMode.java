package uk.co.bithatch.snake.ui.effects;

import uk.co.bithatch.jdraw.RGBCanvas;
import uk.co.bithatch.snake.lib.Colors;

public class BurstyAudioEffectMode implements AudioEffectMode {
	private RGBCanvas canvas;
	private AudioEffectHandler handler;
	private double[] totData = new double[256];
	private int[][] colData = new int[256][3];

	@Override
	public Boolean isNeedsFFT() {
		return true;
	}

	public void frame(double[] audio) {
		int totalCells = canvas.getHeight() * canvas.getWidth();
		float frac = (float) audio.length / (float) totalCells;

		for (int i = 0; i < audio.length; i++) {
			if (totData[i] > 0) {
				totData[i] -= 0.5f;
				if (totData[i] <= 0) {
					totData[i] = 0;
					colData[i] = randomFromColourSet();
				}
			}
		}

		for (int i = 0; i < audio.length; i++) {
			totData[i] += audio[i] *= 2f;
			if (totData[i] > 1)
				totData[i] = 1;
		}
		canvas.clear();
		for (int i = 0; i < canvas.getHeight(); i++) {
			for (int j = 0; j < canvas.getWidth(); j++) {
				int idx = (int) ((((float) i * (float) canvas.getWidth()) + (float) j) * frac);
				canvas.setColour(Colors.getInterpolated(RGBCanvas.BLACK, colData[idx], (float) totData[idx]));
				canvas.plot(j, i);
			}
		}
	}

	@Override
	public void init(RGBCanvas canvas, AudioEffectHandler handler) {
		this.handler = handler;
		this.canvas = canvas;
		for (int i = 0; i < colData.length; i++) {
			colData[i] = randomFromColourSet();
		}
		update();

	}

	private int[] randomFromColourSet() {
		double rnd = Math.random();
		if (rnd < 0.334) {
			return handler.getColor1();
		} else if (rnd < 0.667) {
			return handler.getColor2();
		} else {
			return handler.getColor3();
		}
	}

	@Override
	public void update() {
	}

}