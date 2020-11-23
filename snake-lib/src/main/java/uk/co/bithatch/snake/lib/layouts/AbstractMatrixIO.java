package uk.co.bithatch.snake.lib.layouts;

import uk.co.bithatch.snake.lib.Region;

public abstract class AbstractMatrixIO extends AbstractIO implements MatrixIO {
	private int matrixX;
	private int matrixY;

	public int getMatrixX() {
		return matrixX;
	}

	public void setMatrixX(int matrixX) {
		this.matrixX = matrixX;
		fireChanged();
	}

	public int getMatrixY() {
		return matrixY;
	}

	public void setMatrixY(int matrixY) {
		this.matrixY = matrixY;
		fireChanged();
	}

	@Override
	public String toString() {
		return "AbstractMatrixIO [matrixX=" + matrixX + ", matrixY=" + matrixY + ", getLabel()=" + getLabel()
				+ ", getWidth()=" + getWidth() + ", isDisabled()=" + isDisabled() + ", getX()=" + getX() + ", getY()="
				+ getY() + "]";
	}

}
