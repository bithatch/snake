package uk.co.bithatch.snake.lib.effects;

import java.util.prefs.Preferences;

public abstract class Effect implements Cloneable {

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public final void load(Preferences prefs) {
		Preferences node = prefs.node(getClass().getSimpleName());
		onLoad(node);
	}

	public final void save(Preferences prefs) {
		Preferences node = prefs.node(getClass().getSimpleName());
		onSave(node);
	}

	protected void onLoad(Preferences prefs) {
	}

	protected void onSave(Preferences prefs) {
	}
}
