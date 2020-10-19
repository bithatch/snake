
package uk.co.bithatch.snake.lib;

public interface Region extends Item, Lit {

	public enum Name {
		LOGO, SCROLL, LEFT, RIGHT, CHROMA, BW2013, CUSTOM;
	}

	Device getDevice();

	void load(String path) throws Exception;

	Name getName();

}
