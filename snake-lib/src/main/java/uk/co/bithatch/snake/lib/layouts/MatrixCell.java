package uk.co.bithatch.snake.lib.layouts;

import java.util.Objects;

import uk.co.bithatch.snake.lib.Region;

public class MatrixCell extends AbstractMatrixIO implements RegionIO {

	private int width;
	private boolean disabled;
	private Region.Name region = Region.Name.CHROMA;

	public MatrixCell() {
		super();
	}

	public MatrixCell(DeviceView view) {
		super(view);
	}

	public MatrixCell(MatrixCell cell) {
		super(cell);
		width = cell.width;
		disabled = cell.disabled;
		region = cell.region;
	}

	public Region.Name getRegion() {
		return region;
	}

	public void setRegion(Region.Name region) {
		if (!Objects.equals(region, this.region)) {
			this.region = region;
			fireChanged();
		}
	}

	public boolean isDisabled() {
		return disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
		fireChanged();
	}

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
		fireChanged();
	}

	@Override
	public String toString() {
		return "MatrixCell [region=" + region + ", width=" + width + ", disabled=" + disabled + ", getMatrixX()="
				+ getMatrixX() + ", getMatrixY()=" + getMatrixY() + ", getLabel()=" + getLabel() + ", getX()=" + getX()
				+ ", getY()=" + getY() + "]";
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new MatrixCell(this);
	}
}
