package uk.co.bithatch.snake.lib;

public enum DeviceType {
	MOUSE, MOUSEMAT, KEYBOARD, KEYPAD, CORE, HEADSET, ACCESSORY, UNRECOGNISED;

	public static DeviceType[] concreteTypes() {
		return new DeviceType[] { MOUSE, MOUSEMAT, KEYBOARD, KEYPAD, CORE, HEADSET, ACCESSORY };
	}
}
