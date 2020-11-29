package uk.co.bithatch.snake.lib.layouts;

public class LED extends AbstractMatrixIO {

	public LED() {
		super();
	}
	
	public LED(DeviceView view) {
		super(view);
	}
	
	public LED(LED led) {
		super(led);
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new LED(this);
	}
}
