package uk.co.bithatch.snake.ui.widgets;

import uk.co.bithatch.snake.lib.Device;

public class MapProfileLEDs extends AbstractProfileLEDs {

	private Device device;

	public MapProfileLEDs(Device device) {
		this.device = device;
	}

	protected void setLEDState(boolean[] state) {
		device.getActiveProfile().getActiveMap().setLEDs(state);
	}

	protected boolean[] getLEDState() {
		return device.getActiveProfile().getActiveMap().getLEDs();
	}
}
