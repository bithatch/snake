package uk.co.bithatch.snake.ui;

import java.util.Arrays;
import java.util.Collection;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import javafx.scene.paint.Color;
import uk.co.bithatch.snake.ui.addons.Theme;

public class Configuration implements PreferenceChangeListener {

	public static final String PREF_THEME = "theme";
	public static final String PREF_TRANSPARENCY = "transparency";
	public static final String PREF_H = "h";
	public static final String PREF_W = "w";
	public static final String PREF_Y = "y";
	public static final String PREF_X = "x";
	public static final String PREF_AUDIO_GAIN = "audioGain";
	public static final String PREF_AUDIO_FFT = "audioFFT";
	public static final String PREF_AUDIO_FPS = "audioFPS";
	public static final String PREF_AUDIO_SOURCE = "audioSource";
	public static final String PREF_TURN_OFF_ON_EXIT = "turnOffOnExit";
	public static final String PREF_DECORATED = "decorated";
	public static final String PREF_TRAY_ICON = "trayIcon";
	public static final String PREF_WHEN_LOW = "whenLow";
	public static final String PREF_SHOW_BATTERY = "showBattery";

	public enum TrayIcon {
		OFF, AUTO, DARK, LIGHT, COLOR
	}

	private Preferences node;
	private Theme theme;
	private boolean decorated;
	private boolean turnOffOnExit;
	private int x, y, w, h;
	private String audioSource;
	private int audioFPS;
	private boolean audioFFT;
	private float audioGain;
	private int transparency;
	private boolean showBattery;
	private boolean whenLow;
	private TrayIcon trayIcon;

	Configuration(Preferences node, App context) {
		this.node = node;

		showBattery = node.getBoolean(PREF_SHOW_BATTERY, true);
		whenLow = node.getBoolean(PREF_WHEN_LOW, true);
		trayIcon = TrayIcon.valueOf(node.get(PREF_TRAY_ICON, TrayIcon.AUTO.name()));
		decorated = node.getBoolean(PREF_DECORATED, false);
		turnOffOnExit = node.getBoolean(PREF_TURN_OFF_ON_EXIT, true);
		audioSource = node.get(PREF_AUDIO_SOURCE, "");
		audioFPS = node.getInt(PREF_AUDIO_FPS, 10);
		audioFFT = node.getBoolean(PREF_AUDIO_FFT, false);
		audioGain = node.getFloat(PREF_AUDIO_GAIN, 1);

		x = node.getInt(PREF_X, 0);
		y = node.getInt(PREF_Y, 0);
		w = node.getInt(PREF_W, 0);
		h = node.getInt(PREF_H, 0);
		transparency = node.getInt(PREF_TRANSPARENCY, 0);

		String themeName = node.get(PREF_THEME, "");
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
		theme = selTheme;

		node.addPreferenceChangeListener(this);
	}

	public boolean hasBounds() {
		try {
			return Arrays.asList(node.keys()).contains(PREF_X);
		} catch (BackingStoreException e) {
			return false;
		}
	}

	public Theme getTheme() {
		return theme;
	}

	public void setTheme(Theme theme) {
		this.theme = theme;
		node.put(PREF_THEME, theme.getId());
	}

	public boolean isDecorated() {
		return decorated;
	}

	public void setDecorated(boolean decorated) {
		this.decorated = decorated;
		node.putBoolean(PREF_DECORATED, decorated);
	}

	public boolean isTurnOffOnExit() {
		return turnOffOnExit;
	}

	public void setTurnOffOnExit(boolean turnOffOnExit) {
		this.turnOffOnExit = turnOffOnExit;
		node.putBoolean(PREF_TURN_OFF_ON_EXIT, turnOffOnExit);
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
		node.putInt(PREF_X, x);
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		this.y = y;
		node.putInt(PREF_Y, y);
	}

	public int getW() {
		return w;
	}

	public void setW(int w) {
		this.w = w;
		node.putInt(PREF_W, w);
	}

	public int getH() {
		return h;
	}

	public void setH(int h) {
		this.h = h;
		node.putInt(PREF_H, h);
	}

	public String getAudioSource() {
		return audioSource;
	}

	public void setAudioSource(String audioSource) {
		this.audioSource = audioSource;
		node.put(PREF_AUDIO_SOURCE, audioSource);
	}

	public int getAudioFPS() {
		return audioFPS;
	}

	public void setAudioFPS(int audioFPS) {
		this.audioFPS = audioFPS;
		node.putInt(PREF_AUDIO_FPS, audioFPS);
	}

	public boolean isAudioFFT() {
		return audioFFT;
	}

	public void setAudioFFT(boolean audioFFT) {
		this.audioFFT = audioFFT;
		node.putBoolean(PREF_AUDIO_FFT, audioFFT);
	}

	public float getAudioGain() {
		return audioGain;
	}

	public void setAudioGain(float audioGain) {
		this.audioGain = audioGain;
		node.putFloat(PREF_AUDIO_GAIN, audioGain);
	}

	public int getTransparency() {
		return transparency;
	}

	public void setTransparency(int transparency) {
		this.transparency = transparency;
		node.putInt(PREF_TRANSPARENCY, transparency);
	}

	public boolean isShowBattery() {
		return showBattery;
	}

	public void setShowBattery(boolean showBattery) {
		this.showBattery = showBattery;
		node.putBoolean(PREF_SHOW_BATTERY, showBattery);
	}

	public boolean isWhenLow() {
		return whenLow;
	}

	public void setWhenLow(boolean whenLow) {
		this.whenLow = whenLow;
		node.putBoolean(PREF_WHEN_LOW, whenLow);
	}

	public TrayIcon getTrayIcon() {
		return trayIcon;
	}
	
	public Preferences getNode() {
		return node;
	}

	public void setTrayIcon(TrayIcon trayIcon) {
		this.trayIcon = trayIcon;
		node.put(PREF_TRAY_ICON, trayIcon.name());
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

	@Override
	public void preferenceChange(PreferenceChangeEvent evt) {
		if (evt.getKey().equals(PREF_DECORATED)) {
			if (evt.getNewValue().equals("true")) {
				node.putInt(PREF_TRANSPARENCY, 0);
			}
		}
	}
}
