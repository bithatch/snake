package uk.co.bithatch.snake.lib.animation;

public class AudioParameters {
	private int low = 0;
	private int high = 255;
	private float gain = 1.0f;

	public int getLow() {
		return low;
	}

	public void setLow(int low) {
		this.low = Math.max(0, Math.min(255, low));
	}

	public int getHigh() {
		return high;
	}

	public void setHigh(int high) {
		this.high = Math.max(0, Math.min(255, high));
	}

	public float getGain() {
		return gain;
	}

	public void setGain(float gain) {
		this.gain = Math.max(0.0f, Math.min(20.0f, gain));
	}
}