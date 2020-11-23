package uk.co.bithatch.snake.lib.effects;

import java.util.Arrays;

public class Matrix extends Effect {

	private int[][][] cells;

	public Matrix() {
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Matrix other = (Matrix) obj;
		if (!Arrays.deepEquals(cells, other.cells))
			return false;
		return true;
	}

	public int[] getCell(int row, int col) {
		if (cells == null || row >= cells.length)
			return new int[] { 0, 0, 0 };
		int[][] rowData = cells[row];
		if (rowData == null || col >= rowData.length)
			return new int[] { 0, 0, 0 };
		return rowData[col];
	}

	public int[][][] getCells() {
		return cells;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.deepHashCode(cells);
		return result;
	}

	public void setCells(int[][][] cells) {
		this.cells = cells;
	}

	@Override
	public String toString() {
		return "Matrix [cells=" + Arrays.toString(cells) + "]";
	}

	public void clear() {
		if (cells != null && cells.length > 0) {
			cells = new int[cells.length][cells[0].length][3];
		}
	}

}
