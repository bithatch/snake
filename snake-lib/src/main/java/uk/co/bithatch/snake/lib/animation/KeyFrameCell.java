package uk.co.bithatch.snake.lib.animation;

import java.util.Arrays;

import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.animation.KeyFrame.KeyFrameCellSource;

public class KeyFrameCell {
	private int[] values = KeyFrame.BLACK;
	private Interpolation interpolation = Interpolation.keyframe;
	private KeyFrameCellSource[] sources = new KeyFrameCellSource[] { KeyFrameCellSource.COLOR,
			KeyFrameCellSource.COLOR, KeyFrameCellSource.COLOR };

	public KeyFrameCell(int[] rgb) {
		this(rgb, KeyFrameCellSource.COLOR);
	}

	public KeyFrameCell(int[] rgb, KeyFrameCellSource... sources) {
		super();
		if (sources.length == 1) {
			sources = new KeyFrameCellSource[] { sources[0], sources[0], sources[0] };
		}
		if (sources.length != 3)
			throw new IllegalArgumentException("Must always specify 3 or 1 sources.");
		this.sources = sources;
		this.values = rgb == null ? new int[3] : new int[] { rgb[0], rgb[1], rgb[2] };
	}

	public KeyFrameCell(KeyFrameCell otherCell) {
		copyFrom(otherCell);
	}

	public KeyFrameCellSource[] getSources() {
		return sources;
	}

	public void setSources(KeyFrameCellSource[] sources) {
		this.sources = sources;
	}

	public Interpolation getInterpolation() {
		return interpolation;
	}

	public void setInterpolation(Interpolation interpolate) {
		if (interpolate == null)
			throw new IllegalArgumentException();
		this.interpolation = interpolate;
	}

	public int[] getValues() {
		return values;
	}

	public void setValues(int[] values) {
		this.values = values;
	}

	@Override
	public String toString() {
		return "KeyFrameCell [source=" + Arrays.asList(sources) + ", rgb=" + Colors.toHex(values) + "]";
	}

	public void copyFrom(KeyFrameCell cell) {
		this.values = cell.values;
		this.interpolation = cell.interpolation;
		this.sources = cell.sources;
	}
}