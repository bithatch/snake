package uk.co.bithatch.snake.ui.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.prefs.Preferences;

public class Prefs {
	
	public final static String PREF_DECORATED = "decorated";
	public final static boolean PREF_DECORATED_DEFAULT = false;
	
	
	public final static Set<String> addToStringSet(Preferences node, String key, String value) {
		Set<String> set = new LinkedHashSet<>(getStringSet(node, key));
		set.add(value);
		setStringCollection(node, key, set);
		return set;
	}

	public final static Set<String> removeFromStringSet(Preferences node, String key, String value) {
		Set<String> set = new LinkedHashSet<>(getStringSet(node, key));
		set.remove(value);
		setStringCollection(node, key, set);
		return set;
	}

	public final static Set<String> getStringSet(Preferences node, String key) {
		return getStringSet(node, key, Collections.emptySet());
	}

	public final static Set<String> getStringSet(Preferences node, String key, Set<String> defaultValue) {
		String val = node.get(key, "");
		if (val.equals(""))
			return defaultValue;
		else
			return new LinkedHashSet<>(Arrays.asList(val.split(",")));
	}

	public final static String[] getStringList(Preferences node, String key, String... defaultValue) {
		String val = node.get(key, "");
		if (val.equals(""))
			return defaultValue;
		else
			return val.split(",");
	}

	public final static void setStringCollection(Preferences node, String key, Collection<String> values) {
		node.put(key, String.join(",", values));
	}

	public final static void setStringList(Preferences node, String key, String... values) {
		node.put(key, String.join(",", values));
	}
}
