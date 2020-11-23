package uk.co.bithatch.snake.lib.layouts;

import uk.co.bithatch.snake.lib.Region;

public interface MatrixIO extends IO {

	int getMatrixX();
	
	int getMatrixY();
	
	void setMatrixX(int matrixX);
	
	void setMatrixY(int matrixY);

	default void setMatrixXY(Cell pos) {
		setMatrixX(pos.getX());
		setMatrixY(pos.getY());
	}
	
	default Cell getMatrixXY() {
		return new Cell(getMatrixX(), getMatrixY());
	}
}
