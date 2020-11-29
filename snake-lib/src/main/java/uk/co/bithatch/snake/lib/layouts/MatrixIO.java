package uk.co.bithatch.snake.lib.layouts;

public interface MatrixIO extends IO {

	default boolean isMatrixLED() {
		return getMatrixX() > -1 && getMatrixY() > -1;
	}

	int getMatrixX();

	int getMatrixY();

	void setMatrixX(int matrixX);

	void setMatrixY(int matrixY);

	default void setMatrixXY(int x, int y) {
		setMatrixX(x);
		setMatrixY(y);
	}

	default void setMatrixXY(Cell pos) {
		if (pos == null) {
			setMatrixX(-1);
			setMatrixY(-1);
		} else {
			setMatrixX(pos.getX());
			setMatrixY(pos.getY());
		}
	}

	default Cell getMatrixXY() {
		return getMatrixX() == -1 || getMatrixY() == -1 ? null : new Cell(getMatrixX(), getMatrixY());
	}

	MatrixCell getMatrixCell();

}
