package uk.co.bithatch.snake.lib;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class Json {

	public static String[] toStringArray(JsonArray arr) {
		List<String> l = new ArrayList<>();
		for (JsonElement el : arr) {
			l.add(el.getAsString());
		}
		return l.toArray(new String[0]);
	}

	public static JsonArray toStringJsonArray(String... els) {
		JsonArray arr = new JsonArray();
		for (String e : els)
			arr.add(e);
		return arr;
	}

	public static JsonElement toJson(Path fileName) throws IOException {
		try (BufferedReader r = Files.newBufferedReader(fileName)) {
			return JsonParser.parseReader(r);
		}
	}

	public static JsonElement toJson(InputStream in) throws IOException {
		try (BufferedReader r = new BufferedReader(new InputStreamReader(in))) {
			return JsonParser.parseReader(r);
		}
	}
}
