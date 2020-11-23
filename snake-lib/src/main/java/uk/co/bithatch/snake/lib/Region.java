
package uk.co.bithatch.snake.lib;

public interface Region extends Item, Lit {

	public enum Name {
		BACKLIGHT, CHROMA, LEFT, LOGO, RIGHT, SCROLL;
	}

	Device getDevice();

	Name getName();

}
