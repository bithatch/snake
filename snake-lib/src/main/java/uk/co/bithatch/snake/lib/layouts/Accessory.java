package uk.co.bithatch.snake.lib.layouts;

public class Accessory extends AbstractIO {

	public enum AccessoryType {
		PROFILES;
	}

	private AccessoryType type = AccessoryType.PROFILES;

	public Accessory() {
		super();
	}

	public Accessory(DeviceView view) {
		super(view);
	}

	public Accessory(Accessory key) {
		super(key);
		type = key.type;
	}

	public AccessoryType getAccessory() {
		return type;
	}

	public void setAccessory(AccessoryType type) {
		this.type = type;
		fireChanged();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new Accessory(this);
	}

	@Override
	public String getDefaultLabel() {
		return type == null ? getClass().getSimpleName() : toName(type.name());
	}

}
