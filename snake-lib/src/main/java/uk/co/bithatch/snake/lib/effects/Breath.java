package uk.co.bithatch.snake.lib.effects;

import java.util.Arrays;
import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Colors;

public class Breath extends Effect {

	public enum Mode {
		SINGLE, DUAL, RANDOM
	}

	private Mode mode = Mode.SINGLE;

	private int[] color = new int[] { 0, 255, 0 };
	private int[] color1 = new int[] { 0, 255, 0 };
	private int[] color2 = new int[] { 0, 0, 255 };

	public Breath() {

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

	public int[] getColor1() {
		return color1;
	}

	public void setColor1(int[] color1) {
		this.color1 = color1;
	}

	public int[] getColor2() {
		return color2;
	}

	public void setColor2(int[] color2) {
		this.color2 = color2;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(color);
		result = prime * result + Arrays.hashCode(color1);
		result = prime * result + Arrays.hashCode(color2);
		result = prime * result + ((mode == null) ? 0 : mode.hashCode());
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
		Breath other = (Breath) obj;
		if (!Arrays.equals(color, other.color))
			return false;
		if (!Arrays.equals(color1, other.color1))
			return false;
		if (!Arrays.equals(color2, other.color2))
			return false;
		if (mode != other.mode)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Breath [mode=" + mode + ", color=" + Arrays.toString(color) + ", color1=" + Arrays.toString(color1)
				+ ", color2=" + Arrays.toString(color2) + "]";
	}

	@Override
	protected void onSave(Preferences prefs) {
		prefs.put("mode", mode.name());
		prefs.put("color", Colors.toHex(color));
		prefs.put("color1", Colors.toHex(color1));
		prefs.put("color2", Colors.toHex(color2));
	}

	@Override
	protected void onLoad(Preferences prefs) {
		mode = Mode.valueOf(prefs.get("mode", Mode.RANDOM.name()));
		color = Colors.fromHex(prefs.get("color", "#00ff00"));
		color1 = Colors.fromHex(prefs.get("color2", "#00ff00"));
		color2 = Colors.fromHex(prefs.get("color1", "#0000ff"));
	}

}
