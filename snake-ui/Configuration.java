package uk.co.bithatch.snake.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.FloatProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;
import uk.co.bithatch.snake.ui.addons.Theme;

public class Configuration {

	public enum TrayIcon {
		OFF, AUTO, DARK, LIGHT, COLOR
	}
	

//	private static final float GAIN = 1.0f;

	private Property<Theme> theme = new SimpleObjectProperty<>();
	private BooleanProperty decorated = new SimpleBooleanProperty();
	private BooleanProperty turnOffOnExit = new SimpleBooleanProperty();
	private IntegerProperty x = new SimpleIntegerProperty();
	private IntegerProperty y = new SimpleIntegerProperty();
	private IntegerProperty w = new SimpleIntegerProperty();
	private IntegerProperty h = new SimpleIntegerProperty();
	private IntegerProperty audioSource = new SimpleIntegerProperty();
	private IntegerProperty audioFPS = new SimpleIntegerProperty();
	private BooleanProperty audioFFT = new SimpleBooleanProperty();
	private FloatProperty audioGain = new SimpleFloatProperty();
	private IntegerProperty transparency = new SimpleIntegerProperty();
	private BooleanProperty showBattery = new SimpleBooleanProperty();
	private BooleanProperty whenLow = new SimpleBooleanProperty();
	private Property<TrayIcon> trayIcon = new SimpleObjectProperty<>();
	private Preferences node;

	class ColorPreferenceUpdateChangeListener implements ChangeListener<Color> {

		private Preferences node;
		private String key;

		ColorPreferenceUpdateChangeListener(Preferences node, String key) {
			this.node = node;
			this.key = key;
		}

		@Override
		public void changed(ObservableValue<? extends Color> observable, Color oldValue, Color newValue) {
			putColor(key, node, newValue);
		}

	}

	class ThemePreferenceUpdateChangeListener implements ChangeListener<Theme> {

		private Preferences node;
		private String key;

		ThemePreferenceUpdateChangeListener(Preferences node, String key) {
			this.node = node;
			this.key = key;
		}

		@Override
		public void changed(ObservableValue<? extends Theme> observable, Theme oldValue, Theme newValue) {
			node.put(key, newValue.getId());
		}

	}

	class BooleanPreferenceUpdateChangeListener implements ChangeListener<Boolean> {

		private Preferences node;
		private String key;

		BooleanPreferenceUpdateChangeListener(Preferences node, String key) {
			this.node = node;
			this.key = key;
		}

		@Override
		public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
			node.putBoolean(key, newValue);
		}

	}

	class IntegerPreferenceUpdateChangeListener implements ChangeListener<Number> {

		private Preferences node;
		private String key;

		IntegerPreferenceUpdateChangeListener(Preferences node, String key) {
			this.node = node;
			this.key = key;
		}

		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			node.putInt(key, newValue.intValue());
		}

	}

	class FloatPreferenceUpdateChangeListener implements ChangeListener<Number> {

		private Preferences node;
		private String key;

		FloatPreferenceUpdateChangeListener(Preferences node, String key) {
			this.node = node;
			this.key = key;
		}

		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			node.putFloat(key, newValue.floatValue());
		}

	}

	class TrayIconPreferenceUpdateChangeListener implements ChangeListener<TrayIcon> {

		private Preferences node;
		private String key;

		TrayIconPreferenceUpdateChangeListener(Preferences node, String key) {
			this.node = node;
			this.key = key;
		}

		@Override
		public void changed(ObservableValue<? extends TrayIcon> observable, TrayIcon oldValue, TrayIcon newValue) {
			node.put(key, newValue.name());
		}

	}

	Configuration(Preferences node, App context) {
		this.node = node;

		showBattery.setValue(node.getBoolean("showBattery", true));
		showBattery.addListener(new BooleanPreferenceUpdateChangeListener(node, "showBattery"));

		whenLow.setValue(node.getBoolean("whenLow", true));
		whenLow.addListener(new BooleanPreferenceUpdateChangeListener(node, "whenLow"));

		trayIcon.setValue(TrayIcon.valueOf(node.get("trayIcon", TrayIcon.AUTO.name())));
		trayIcon.addListener(new TrayIconPreferenceUpdateChangeListener(node, "trayIcon"));

		decorated.setValue(node.getBoolean("decorated", false));
		decorated.addListener(new BooleanPreferenceUpdateChangeListener(node, "decorated"));
		decorated.addListener((e) -> {
			if (decorated.get()) {
				transparency.setValue(0);
			}
		});
		turnOffOnExit.setValue(node.getBoolean("turnOffOnExit", true));
		turnOffOnExit.addListener(new BooleanPreferenceUpdateChangeListener(node, "turnOffOnExit"));

		audioSource.setValue(node.getInt("audioSource", 0));
		audioSource.addListener(new IntegerPreferenceUpdateChangeListener(node, "audioSource"));

		audioFPS.setValue(node.getInt("audioFPS", 10));
		audioFPS.addListener(new IntegerPreferenceUpdateChangeListener(node, "audioFPS"));

		audioFFT.setValue(node.getBoolean("audioFFT", true));
		audioFFT.addListener(new BooleanPreferenceUpdateChangeListener(node, "audioFFT"));

		audioGain.setValue(node.getInt("audioGain", 1));
		audioGain.addListener(new FloatPreferenceUpdateChangeListener(node, "audioGain"));

		x.setValue(node.getInt("x", 0));
		x.addListener(new IntegerPreferenceUpdateChangeListener(node, "x"));

		y.setValue(node.getInt("y", 0));
		y.addListener(new IntegerPreferenceUpdateChangeListener(node, "y"));

		w.setValue(node.getInt("w", 0));
		w.addListener(new IntegerPreferenceUpdateChangeListener(node, "w"));

		h.setValue(node.getInt("h", 0));
		h.addListener(new IntegerPreferenceUpdateChangeListener(node, "h"));

		transparency.setValue(node.getInt("transparency", 0));
		transparency.addListener(new IntegerPreferenceUpdateChangeListener(node, "transparency"));
		String themeName = node.get("theme", "");
		Collection<Theme> themes = context.getAddOnManager().getThemes();
		if (themes.isEmpty())
			throw new IllegalStateException("No themes. Please add a theme module to the classpath or modulepath.");
		Theme firstTheme = themes.iterator().next();
		if (themeName.equals("")) {
			themeName = firstTheme.getId();
		}
		Theme selTheme = context.getAddOnManager().getTheme(themeName);
		if (selTheme == null && !themeName.equals(firstTheme.getId()))
			selTheme = firstTheme;
		theme.setValue(selTheme);
		theme.addListener(new ThemePreferenceUpdateChangeListener(node, "theme"));
	}

	public boolean hasBounds() {
		try {
			return Arrays.asList(node.keys()).contains("x");
		} catch (BackingStoreException e) {
			return false;
		}
	}

		return audioGain;
	}


	static void putColor(String key, Preferences p, Color color) {
		p.putDouble(key + "_r", color.getRed());
		p.putDouble(key + "_g", color.getGreen());
		p.putDouble(key + "_b", color.getBlue());
		p.putDouble(key + "_a", color.getOpacity());
	}

	static Color getColor(String key, Preferences p, Color defaultColour) {
		return new Color(p.getDouble(key + "_r", defaultColour == null ? 1.0 : defaultColour.getRed()),
				p.getDouble(key + "_g", defaultColour == null ? 1.0 : defaultColour.getGreen()),
				p.getDouble(key + "_b", defaultColour == null ? 1.0 : defaultColour.getBlue()),
				p.getDouble(key + "_a", defaultColour == null ? 1.0 : defaultColour.getOpacity()));
	}
}
