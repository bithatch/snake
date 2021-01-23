package uk.co.bithatch.snake.ui.drawing;

import uk.co.bithatch.jdraw.Backing;

public class MatrixBacking implements Backing {

	private int[][][] matrix;
	private boolean rot;
	private int width;
	private int height;

	public MatrixBacking(int width, int height, int[][][] matrix) {
		this.matrix = matrix;
		if (height < 3) {
			rot = true;
			this.height = width;
			this.width = height;
		} else {
			this.width = width;
			this.height = height;
		}
	}

	@Override
	public void plot(int x, int y, int[] rgb) {
		if (rot) {
			matrix[x][y][0] = rgb[0];
			matrix[x][y][1] = rgb[1];
			matrix[x][y][2] = rgb[02];
		} else {
			matrix[y][x][0] = rgb[0];
			matrix[y][x][1] = rgb[1];
			matrix[y][x][2] = rgb[02];
		}
	}

	@Override
	public int getWidth() {
		return width;
	}

	@Override
	public int getHeight() {
		return height;
	}

}
