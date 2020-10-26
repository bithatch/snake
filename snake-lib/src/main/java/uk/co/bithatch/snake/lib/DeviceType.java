package uk.co.bithatch.snake.lib;

public enum DeviceType {
	ACCESSORY, CORE, HEADSET, KEYBOARD, KEYPAD, MOUSE, MOUSEMAT, UNRECOGNISED;

	public static DeviceType[] concreteTypes() {
		return new DeviceType[] { MOUSE, MOUSEMAT, KEYBOARD, KEYPAD, CORE, HEADSET, ACCESSORY };
	}
}
