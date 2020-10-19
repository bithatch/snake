package uk.co.bithatch.snake.lib.effects;

import java.util.Arrays;
import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Colors;

public class Ripple extends Effect {
	public enum Mode {
		SINGLE, RANDOM
	}

	private Mode mode = Mode.SINGLE;

	private int[] color = new int[] { 0, 255, 0 };
	private double refreshRate = 100;

	public Ripple() {
	}

	public Mode getMode() {
		return mode;
	}

	public void setMode(Mode mode) {
		this.mode = mode;
	}

	public int[] getColor() {
		return color;
	}

	public void setColor(int[] color) {
		this.color = color;
	}

	public double getRefreshRate() {
		return refreshRate;
	}

	public void setRefreshRate(double refreshRate) {
		this.refreshRate = refreshRate;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(color);
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
		long temp;
		temp = Double.doubleToLongBits(refreshRate);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Ripple other = (Ripple) obj;
		if (!Arrays.equals(color, other.color))
			return false;
		if (mode != other.mode)
			return false;
		if (Double.doubleToLongBits(refreshRate) != Double.doubleToLongBits(other.refreshRate))
			return false;
		return true;
	}

	@Override
	protected void onSave(Preferences prefs) {
		prefs.put("mode", mode.name());
		prefs.putDouble("refreshRate", refreshRate);
		prefs.put("color", Colors.toHex(color));
	}

	@Override
	protected void onLoad(Preferences prefs) {
		mode = Mode.valueOf(prefs.get("mode", Mode.RANDOM.name()));
		refreshRate = prefs.getDouble("refreshRate", 100);
		color = Colors.fromHex(prefs.get("color", "#00ff00"));
	}

	@Override
	public String toString() {
		return "Ripple [mode=" + mode + ", color=" + Arrays.toString(color) + ", refreshRate=" + refreshRate + "]";
	}
}
