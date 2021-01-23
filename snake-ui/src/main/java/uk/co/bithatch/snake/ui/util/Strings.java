package uk.co.bithatch.snake.ui.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;

public class Strings {

	public static String toSeparatedList(String separator, Iterable<? extends Object> values) {
		StringBuilder b = new StringBuilder();
		for (Object o : values) {
			if (b.length() > 0)
				b.append(separator);
			b.append(String.valueOf(o));
		}
		return b.toString();
	}

	public static List<String> addIfNotAdded(List<String> target, String... source) {
		for (String s : source) {
			if (!target.contains(s))
				target.add(s);
		}
		return target;
	}

	public static List<String> addIfNotAdded(List<String> target, List<String> source) {
		for (String s : source) {
			if (!target.contains(s))
				target.add(s);
		}
		return target;
	}

	public static String toId(String name) {
		return name.toLowerCase().replace(" ", "-").replace("_", "-").replaceAll("[^a-z0-9\\-]", "");
	}

	public static String genericHash(String input) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
			return Base64.getEncoder().encodeToString(hash).replace("/", "").replace("=", "");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean isBlank(String string) {
		return string == null || string.length() == 0;
	}

	public static String[] toStringArray(String separatorRegex, String stringList) {
		return isBlank(stringList) ? new String[0] : stringList.split(separatorRegex);
	}

	public static String defaultIfBlank(String str, String defaultValue) {
		return isBlank(str) ? defaultValue : str;
	}

	public static String basename(String url) {
		int idx = url.lastIndexOf("/");
		if (idx == -1)
			return null;
		else
			return url.substring(idx + 1);
	}

	public static String extension(String url) {
		int idx = url.lastIndexOf(".");
		if (idx == -1)
			return null;
		else
			return url.substring(idx + 1);
	}

	public static String changeExtension(String path, String ext) {
		int idx = path.lastIndexOf(".");
		return idx == -1 ? path + "." + ext : path.substring(0, idx) + "." + ext;
	}

	public static String toName(String name) {
		if (name == null || name.length() == 0)
			return name;
		// TODO bit weak
		return (name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase()).replace('_', ' ');
	}
}
