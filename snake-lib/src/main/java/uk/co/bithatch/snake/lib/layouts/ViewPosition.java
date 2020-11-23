package uk.co.bithatch.snake.lib.layouts;

import uk.co.bithatch.snake.lib.BrandingImage;

public enum ViewPosition {
	TOP, BOTTOM, SIDE_1, SIDE_2, FRONT, BACK, THREE_D_1, THREE_D_2, MATRIX;

	public BrandingImage toBrandingImage() {
		switch (this) {
		case TOP:
			return BrandingImage.TOP;
		case SIDE_1:
		case SIDE_2:
			return BrandingImage.SIDE;
		case THREE_D_1:
		case THREE_D_2:
			return BrandingImage.PERSPECTIVE;
		default:
			return null;
		}
	}
}
