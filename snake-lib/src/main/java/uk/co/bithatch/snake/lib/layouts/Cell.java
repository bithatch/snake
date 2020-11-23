package uk.co.bithatch.snake.lib.layouts;

public class Cell {
	private final int x;
	private final int y;

	public Cell(int x, int y) {
		this.y = y;
		this.x = x;
	}

	public Cell(MatrixIO matrixIO) {
		this.y = matrixIO.getMatrixY();
		this.x = matrixIO.getMatrixX();
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Cell other = (Cell) obj;
		if (y != other.y)
			return false;
		if (x != other.x)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + y;
		result = prime * result + x;
		return result;
	}

	@Override
	public String toString() {
		return "Cell [y=" + y + ", x=" + x + "]";
	}

}