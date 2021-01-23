package uk.co.bithatch.snake.lib.layouts;

public abstract class AbstractMatrixIO extends AbstractIO implements MatrixIO {
	private int matrixX;
	private int matrixY;

	protected AbstractMatrixIO() {
		super();
	}

	protected AbstractMatrixIO(DeviceView view) {
		super(view);
	}

	protected AbstractMatrixIO(AbstractMatrixIO io) {
		super(io);
		matrixX = io.matrixX;
		matrixY = io.matrixY;
	}

	@Override
	public String getDefaultLabel() {
		MatrixCell cell = getMatrixCell();
		if (cell == null || cell == this) {
			DeviceView view = getView();
			if(view != null && view.getLayout().getMatrixWidth() == 1)
				return String.format("%d", matrixY);
			else if(view != null && view.getLayout().getMatrixHeight() == 1)
				return String.format("%d", matrixX);
			else
				return String.format("%d,%d", matrixX, matrixY);
		}
		else
			return cell.getDisplayLabel();
	}

	public int getMatrixX() {
		return matrixX;
	}

	public void setMatrixX(int matrixX) {
		if (matrixX != this.matrixX) {
			if (matrixX != -1 && getView() != null && getView().getLayout() != null
					&& (matrixX < -1 || matrixX >= getView().getLayout().getMatrixWidth()))
				throw new IllegalArgumentException(
						String.format("Matrix X of %d is greater than the matrix width of %d", matrixX,
								getView().getLayout().getMatrixWidth()));
			this.matrixX = matrixX;
			fireChanged();
		}
	}

	public int getMatrixY() {
		return matrixY;
	}

	public void setMatrixY(int matrixY) {
		if (matrixY != this.matrixY) {
			if (matrixY != -1 && getView() != null && getView().getLayout() != null
					&& (matrixY < -1 || matrixY >= getView().getLayout().getMatrixHeight()))
				throw new IllegalArgumentException(
						String.format("Matrix Y of %d is greater than the matrix height of %d", matrixY,
								getView().getLayout().getMatrixHeight()));
			this.matrixY = matrixY;
			fireChanged();
		}
	}

	@Override
	public String toString() {
		return "AbstractMatrixIO [hashCode=" + hashCode() + ",clazz=" + getClass() + ",matrixX=" + matrixX
				+ ", matrixY=" + matrixY + ", getLabel()=" + getLabel() + ", getWidth()=" + getX() + ",y=" + getY()
				+ "]";
	}

	@Override
	public MatrixCell getMatrixCell() {
		DeviceView view = getView();
		if (view == null)
			return null;
		return (MatrixCell) view.getLayout().getViews().get(ViewPosition.MATRIX).getElement(ComponentType.MATRIX_CELL,
				getMatrixX(), getMatrixY());
	}

}
