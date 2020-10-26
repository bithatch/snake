package uk.co.bithatch.snake.lib.effects;

import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

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

	@Override
	protected void onLoad(Preferences prefs) {
		Preferences matrix = prefs.node("matrix");
		int rows = matrix.getInt("rows", 0);
		if (rows == 0) {
			cells = null;
		} else {
			cells = new int[rows][][];
			for (int i = 0; i < rows; i++) {
				var data = matrix.get("row" + i, "");
				if (!data.equals("")) {
					String[] rowRgb = data.split(":");
					int[][] row = new int[rowRgb.length][3];
					int col = 0;
					for (String rgb : rowRgb) {
						row[col][0] = Integer.parseInt(rgb.substring(0, 2), 16);
						row[col][1] = Integer.parseInt(rgb.substring(2, 4), 16);
						row[col][2] = Integer.parseInt(rgb.substring(4, 6), 16);
						col++;
					}
					cells[i] = row;
				}
			}
		}
	}

	@Override
	protected void onSave(Preferences prefs) {
		/* Delete existing matrix */
		Preferences matrix = prefs.node("matrix");
		try {
			matrix.removeNode();
			matrix = prefs.node("matrix");
		} catch (BackingStoreException bse) {
			throw new IllegalStateException("Failed to remove old matrix.", bse);
		}

		if (cells != null) {
			int rowIdx = 0;
			for (int[][] row : cells) {
				StringBuilder b = new StringBuilder();
				if (row != null) {
					for (int[] rgb : row) {
						if (b.length() > 0)
							b.append(":");
						b.append(String.format("%02x%02x%02x", rgb[0], rgb[1], rgb[2]));
					}
					matrix.put("row" + rowIdx, b.toString());
				}
				rowIdx++;
			}
			matrix.putInt("rows", rowIdx);
		}
	}

}
