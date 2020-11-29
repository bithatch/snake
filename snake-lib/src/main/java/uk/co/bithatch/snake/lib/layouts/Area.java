package uk.co.bithatch.snake.lib.layouts;

import uk.co.bithatch.snake.lib.Region;

public class Area extends AbstractIO implements RegionIO {

	private Region.Name region;

	public Area() {
		super();
	}

	public Area(DeviceView view) {
		super(view);
	}

	public Area(Area area) {
		super(area);
		region = area.region;
	}

	public Region.Name getRegion() {
		return region;
	}

	public void setRegion(Region.Name region) {
		this.region = region;
		fireChanged();
	}

	@Override
	public String getDefaultLabel() {
		return region == null ? getClass().getSimpleName() : toName(region.name());
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new Area(this);
	}
}
