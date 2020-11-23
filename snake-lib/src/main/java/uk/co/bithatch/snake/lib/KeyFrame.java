package uk.co.bithatch.snake.lib;

public class KeyFrame {

	private int[][][] frame;
	private long holdFor;
	private long index;
	private Interpolation interpolation = Interpolation.sequence;
	private Sequence sequence;
	private long startFrame;

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

	public KeyFrame(int[][][] frame, long holdFor, Interpolation interpolate) {
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

	public int[] getCell(int row, int col) {
		if (frame == null || row >= frame.length)
			return new int[] { 0, 0, 0 };
		int[][] rowData = frame[row];
		if (rowData == null || col >= rowData.length)
			return new int[] { 0, 0, 0 };
		return rowData[col];
	}

	public long getEndTime() {
		return getActualLength() + getIndex();
	}

	public int[][][] getFrame() {
		return frame;
	}

	public long getHoldFor() {
		return holdFor;
	}

	public long getIndex() {
		return index;
	}

	public int[][][] getInterpolatedFrame(KeyFrame nextFrame, float frac) {
		int[][][] next = nextFrame.getFrame();
		int[][][] newFrame = new int[frame.length][][];

		for (int i = 0; i < frame.length; i++) {
			int[][] thisRow = frame[i];
			int[][] otherRow = next[i];
			int[][] newRow = new int[thisRow.length][3];
			for (int j = 0; j < newRow.length; j++) {
				int[] thisCol = thisRow[j];
				int[] otherCol = otherRow[j];
				int[] newCol = newRow[j];
				newCol[0] = thisCol[0] + (int) (((float) otherCol[0] - (float) thisCol[0]) * frac);
				newCol[1] = thisCol[1] + (int) (((float) otherCol[1] - (float) thisCol[1]) * frac);
				newCol[2] = thisCol[2] + (int) (((float) otherCol[2] - (float) thisCol[2]) * frac);
			}
			newFrame[i] = newRow;
		}
		return newFrame;
	}

	public Interpolation getInterpolation() {
		return interpolation;
	}

	public int[] getOverallColor() {
		return getOverallColor(false);
	}

	public int[] getOverallColor(boolean includeBlack) {
		if (frame == null)
			return Colors.COLOR_BLACK;
		long els = 0;
		long r = 0, g = 0, b = 0;
		for (int[][] row : frame) {
			for (int[] col : row) {
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

	public void setColor(int[] fillColour, int rows, int cols) {
		frame = new int[rows][cols][3];
		for (int[][] row : frame) {
			for (int[] col : row) {
				col[0] = fillColour[0];
				col[1] = fillColour[1];
				col[2] = fillColour[2];
			}
		}
	}

	public void setFrame(int[][][] frame) {
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
}
