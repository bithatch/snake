package uk.co.bithatch.snake.ui;

import java.util.Arrays;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;

public class Configuration {

	public enum TrayIcon {
		OFF, AUTO, DARK, LIGHT, COLOR
	}

	private Property<Color> color = new SimpleObjectProperty<>();
	private BooleanProperty decorated = new SimpleBooleanProperty();
	private IntegerProperty x = new SimpleIntegerProperty();
	private IntegerProperty y = new SimpleIntegerProperty();
	private IntegerProperty w = new SimpleIntegerProperty();
	private IntegerProperty h = new SimpleIntegerProperty();
	private IntegerProperty transparency = new SimpleIntegerProperty();
	private BooleanProperty showBattery = new SimpleBooleanProperty();
	private BooleanProperty whenLow = new SimpleBooleanProperty();
	private Property<TrayIcon> trayIcon = new SimpleObjectProperty<>();
	private Preferences node;

	//
	private final static Configuration DEFAULT_INSTANCE = new Configuration(
			Preferences.userNodeForPackage(Configuration.class));

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

	public Configuration(Preferences node) {
		this.node = node;

		color.setValue(getColor("color", node, Color.web("#000000")));
		color.addListener(new ColorPreferenceUpdateChangeListener(node, "color"));
		color.addListener((e) -> transparency.setValue(calcTrans()));

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

		x.setValue(node.getInt("x", 0));
		x.addListener(new IntegerPreferenceUpdateChangeListener(node, "x"));

		y.setValue(node.getInt("y", 0));
		y.addListener(new IntegerPreferenceUpdateChangeListener(node, "y"));

		w.setValue(node.getInt("w", 0));
		w.addListener(new IntegerPreferenceUpdateChangeListener(node, "w"));

		h.setValue(node.getInt("h", 0));
		h.addListener(new IntegerPreferenceUpdateChangeListener(node, "h"));

		transparency.setValue(calcTrans());
		transparency.addListener((e) -> {
			Color c = color.getValue();
			c = new Color(c.getRed(), c.getGreen(), c.getBlue(),
					((100.0 - transparency.getValue().doubleValue()) / 100.0));
			color.setValue(c);
		});
	}

	private int calcTrans() {
		return 100 - (int) (color.getValue().getOpacity() * 100.0);
	}

	public static Configuration getDefault() {
		return DEFAULT_INSTANCE;
	}

	public boolean hasBounds() {
		try {
			return Arrays.asList(node.keys()).contains("x");
		} catch (BackingStoreException e) {
			return false;
		}
	}

	public IntegerProperty xProperty() {
		return x;
	}

	public IntegerProperty yProperty() {
		return y;
	}

	public IntegerProperty wProperty() {
		return w;
	}

	public IntegerProperty hProperty() {
		return h;
	}

	public IntegerProperty transparencyProperty() {
		return transparency;
	}

	public Property<TrayIcon> trayIconProperty() {
		return trayIcon;
	}

	public Property<Color> colorProperty() {
		return color;
	}

	public Property<Boolean> showBatteryProperty() {
		return showBattery;
	}

	public Property<Boolean> whenLowProperty() {
		return whenLow;
	}

	public Property<Boolean> decoratedProperty() {
		return decorated;
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
