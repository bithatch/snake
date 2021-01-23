package uk.co.bithatch.snake.lib.animation;

import uk.co.bithatch.snake.lib.Colors;

public class KeyFrame {

	static final int[] BLACK = new int[3];

	public enum KeyFrameCellSource {
		COLOR, RANDOM, AUDIO
	}

	private static final KeyFrameCell EMPTY = new KeyFrameCell(BLACK);

	private KeyFrameCell[][] frame;
	private long holdFor;
	private long index;
	private Interpolation interpolation = Interpolation.sequence;
	private Sequence sequence;
	private long startFrame;

	private int[][][] rgbFrame;

	public KeyFrame() {
	}

	public KeyFrame(KeyFrame frame) {
		this.frame = frame.frame;
		this.holdFor = frame.holdFor;
		this.index = frame.index;
		this.interpolation = frame.interpolation;
		this.sequence = frame.sequence;
		this.startFrame = frame.startFrame;
	}

	public KeyFrame(KeyFrameCell[][] frame, long holdFor, Interpolation interpolate) {
		super();
		this.frame = frame;
		this.holdFor = holdFor;
		this.interpolation = interpolate;
	}

	public long getActualFrames() {
		return holdFor == 0 ? 1
				: (long) ((double) sequence.getFps() * ((double) holdFor / 1000.0) / sequence.getSpeed());
	}

	public long getActualLength() {
		return (long) ((double) (holdFor == 0 ? (long) ((1.0 / (float) sequence.getFps()) * 1000.0) : holdFor)
				/ sequence.getSpeed());
	}

//	public boolean isAudioColor(int x, int y) {
//		return getCell(x, y).getSource().equals(KeyFrameCellSource.AUDIO);
//	}
//
//	public boolean isRGB(int x, int y) {
//		return getCell(x, y).getSource().equals(KeyFrameCellSource.COLOR);
//	}
//
//	public boolean isRandomColor(int x, int y) {
//		return getCell(x, y).getSource().equals(KeyFrameCellSource.RANDOM);
//	}

	public KeyFrameCell getCell(int x, int y) {
		if (frame == null || y >= frame.length)
			return EMPTY;
		KeyFrameCell[] rowData = frame[y];
		if (rowData == null || x >= rowData.length)
			return EMPTY;
		return rowData[x];
	}

	public long getEndTime() {
		return getActualLength() + getIndex();
	}

//	public void setRandomColor(int x, int y, int[] rgb) {
//		setCell(x, y, new KeyFrameCell(KeyFrameCellSource.RANDOM, rgb));
//	}
//
//	public void setFixedColor(int x, int y, int[] rgb) {
//		setCell(x, y, new KeyFrameCell(KeyFrameCellSource.COLOR, rgb));
//	}
//
//	public void setAudioColor(int x, int y, int[] rgb) {
//		setCell(x, y, new KeyFrameCell(KeyFrameCellSource.AUDIO, rgb));
//	}

	public void setCell(int x, int y, KeyFrameCell cell) {
		frame[y][x] = cell;
		rgbFrame = null;
	}

	public void setRGB(int x, int y, int[] rrgb) {
		if (frame[y][x] == null)
			setCell(x, y, new KeyFrameCell(rrgb));
		else
			frame[y][x].setValues(rrgb);
		rgbFrame = null;
	}

	public KeyFrameCell[][] getFrame() {
		return frame;
	}

	public int[][][] getRGBFrame(AudioDataProvider audioDataProvider) {
		if (rgbFrame == null) {
			int[][][] f = new int[frame.length][][];
			for (int row = 0; row < frame.length; row++) {
				f[row] = new int[frame[row].length][];
				for (int col = 0; col < frame[row].length; col++) {

					f[row][col] = getGeneratedCellColor(col, row, audioDataProvider);

				}
			}
			rgbFrame = f;
		}
		return rgbFrame;
	}

	public int[] getGeneratedCellColor(int x, int y, AudioDataProvider dataProvider) {
		KeyFrameCell cell = frame[y][x];
		if (cell == null) {
			return BLACK;
		} else {
			int[] values = cell.getValues();
			int[] hsv = new int[3];
			double audioAvg = -1;
			int[] colAsHsv = Colors.toIntHSB(values);

			for (int i = 0; i < 3; i++) {
				switch (cell.getSources()[i]) {
				case AUDIO:
					if (dataProvider == null) {
						hsv[i] = colAsHsv[i];
					} else {
						if (audioAvg == -1) {
							audioAvg = 0;
							double[] data = dataProvider.getSnapshot();
							AudioParameters audioParameters = getSequence().getAudioParameters();
							if (audioParameters == null) {
								for (double d : data)
									audioAvg += d;
							} else {
								for (int j = audioParameters.getLow(); j <= audioParameters.getHigh(); j++) {
									audioAvg += data[j] * audioParameters.getGain();
								}
							}
							audioAvg /= data.length;
						}
						hsv[i] = (int) (audioAvg * 255.0);
					}
					break;
				case RANDOM:
					hsv[i] = (int) (Math.random() * 255.0);
					break;
				default:
					// TODO should be hsv converted to RGB
					hsv[i] = colAsHsv[i];
					break;
				}
			}

			return Colors.toRGB(hsv);
		}
	}

	public long getHoldFor() {
		return holdFor;
	}

	public long getIndex() {
		return index;
	}

	public Interpolation getBestCellInterpolation(int x, int y) {
		Interpolation i = frame[y][x] == null ? null : frame[y][x].getInterpolation();
		return i == null || i.equals(Interpolation.keyframe) ? getBestInterpolation() : i;
	}

	public int[][][] getInterpolatedFrame(KeyFrame nextFrame, float frac, AudioDataProvider dataProvider) {
		int[][][] next = nextFrame.getRGBFrame(dataProvider);
		int[][][] thisRGB = getRGBFrame(dataProvider);

		int[][][] newFrame = new int[frame.length][][];

		for (int y = 0; y < frame.length; y++) {
			int[][] thisRow = thisRGB[y];
			int[][] otherRow = next[y];
			int[][] newRow = new int[thisRow.length][3];
			for (int x = 0; x < newRow.length; x++) {
				Interpolation inter = getBestCellInterpolation(x, y);
				int[] thisCol = thisRow[x];
				int[] otherCol = otherRow[x];
				int[] newCol = newRow[x];

				if (inter.equals(Interpolation.none)) {
					/*
					 * If interpolation is none, then we query the source each frame rather than
					 * interpolate a single value from one from to the next
					 */
					int[] rgb = getGeneratedCellColor(x, y, dataProvider);
					newCol[0] = rgb[0];
					newCol[1] = rgb[1];
					newCol[2] = rgb[2];
				} else {
					float cellFrac = inter.apply(frac);
					newCol[0] = thisCol[0] + (int) (((float) otherCol[0] - (float) thisCol[0]) * cellFrac);
					newCol[1] = thisCol[1] + (int) (((float) otherCol[1] - (float) thisCol[1]) * cellFrac);
					newCol[2] = thisCol[2] + (int) (((float) otherCol[2] - (float) thisCol[2]) * cellFrac);
				}
			}
			newFrame[y] = newRow;
		}
		return newFrame;
	}

	public Interpolation getInterpolation() {
		return interpolation;
	}

	public int[] getOverallColor(AudioDataProvider audio) {
		return getOverallColor(audio, false);
	}

	public int[] getOverallColor(AudioDataProvider audio, boolean includeBlack) {
		if (frame == null)
			return Colors.COLOR_BLACK;
		long els = 0;
		long r = 0, g = 0, b = 0;
		for (int y = 0; y < frame.length; y++) {
			for (int x = 0; x < frame[y].length; x++) {
				int[] col = getGeneratedCellColor(x, y, audio);
				if (col[0] == 0 && col[1] == 0 && col[2] == 0)
					continue;
				els++;
				r += col[0];
				g += col[1];
				b += col[2];
			}
		}
		return els == 0 ? Colors.COLOR_BLACK : new int[] { (int) (r / els), (int) (g / els), (int) (b / els) };
	}

	public Sequence getSequence() {
		return sequence;
	}

	public long getStartFrame() {
		return startFrame;
	}

	public void setColor(int[] fillColor, int rows, int cols) {
		frame = new KeyFrameCell[rows][cols];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				frame[i][j] = new KeyFrameCell(fillColor);
			}
		}
		rgbFrame = null;
	}

	public void setFrame(KeyFrameCell[][] frame) {
		this.frame = frame;
	}

	public void setHoldFor(long holdFor) {
		try {
			this.holdFor = holdFor;
		} finally {
			if (sequence != null) {
				sequence.rebuildTimestats();
			}
		}
	}

	public void setInterpolation(Interpolation interpolate) {
		this.interpolation = interpolate;
	}

	void setIndex(long index) {
		this.index = index;
	}

	void setSequence(Sequence sequence) {
		this.sequence = sequence;
	}

	void setStartFrame(long startFrame) {
		this.startFrame = startFrame;
	}

//	public int getRandomCells() {
//		int c = 0;
//		for (int y = 0; y < frame.length; y++) {
//			for (int x = 0; x < frame[y].length; x++) {
//				if (isRandomColor(x, y)) {
//					c++;
//				}
//			}
//		}
//		return c;
//	}
//
//	public int getAudioCells() {
//		int c = 0;
//		for (int y = 0; y < frame.length; y++) {
//			for (int x = 0; x < frame[y].length; x++) {
//				if (isAudioColor(x, y)) {
//					c++;
//				}
//			}
//		}
//		return c;
//	}
//
//	public int getRGBCells() {
//		int c = 0;
//		for (int y = 0; y < frame.length; y++) {
//			for (int x = 0; x < frame[y].length; x++) {
//				if (isRGB(x, y)) {
//					c++;
//				}
//			}
//		}
//		return c;
//	}

	public void played() {
		rgbFrame = null;
	}

	public Interpolation getBestInterpolation() {
		return interpolation == null || interpolation.equals(Interpolation.sequence) ? getSequence().getInterpolation()
				: interpolation;
	}

}
