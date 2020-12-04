package uk.co.bithatch.snake.lib;

import java.util.HashMap;
import java.util.Map;

@Deprecated
public enum Key {
	APOSTROPHE, BACKSLASH, BACKSPACE, BACKTICK, CAPSLK, COMMA, CTXMENU, DASH, DELETE, DOWNARROW, END, ENTER, EQUALS,
	ESC, FN, FORWARDSLASH, GAMEMODE, HOME, INS, LEFTALT, LEFTARROW, LEFTCTRL, LEFTSHIFT, LEFTSQUAREBRACKET, M1, M2, M3,
	M4, M5, MACROMODE, NP0, NP1, NP2, NP3, NP4, NP5, NP6, NP7, NP8, NP9, NPASTERISK, NPDASH, NPFORWARDSLASH, NPPERIOD,
	NPPLUS, NUMLK, PAGEDOWN, PAGEUP, PAUSE, PERIOD, POUNDSIGN, PRTSCR, RETURN, RIGHTALT, RIGHTARROW, RIGHTCTRL,
	RIGHTSHIFT, RIGHTSQUAREBRACKET, SCRLK, SEMICOLON, SPACE, SUPER, TAB, UPARROW, ONE("1"), TWO("2"), THREE("3"),
	FOUR("4"), FIVE("5"), SIX("6"), SEVEN("7"), EIGHT("8"), NINE("9"), TEN("10"), ELEVEN("11"), TWELVE("12"),
	THIRTEEN("13"), FOURTEEN("14"), FIFTEEN("15"), SIXTEEN("16"), SEVENTEEN("17"), EIGHTEEN("18"), NINETEEN("19"),
	MODE_SWITCH, THUMB, UP, DOWN, LEFT, RIGHT;

	String nativeKeyName;

	Key() {
		this(null);
	}

	Key(String nativeKeyName) {
		this.nativeKeyName = nativeKeyName;
		Key.NameMap.codeToName.put(nativeKeyName, this);
	}

	public String nativeKeyName() {
		return nativeKeyName == null ? name() : nativeKeyName;
	}

	public static Key fromNativeKeyName(String nativeKeyName) {
		Key key = Key.NameMap.codeToName.get(nativeKeyName);
		if (key == null)
			throw new IllegalArgumentException(String.format("No input event with code %s", nativeKeyName));
		return key;
	}

	static class NameMap {

		final static Map<String, Key> codeToName = new HashMap<>();
	}
}
