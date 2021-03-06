package uk.co.bithatch.snake.lib.layouts;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

@Deprecated
public class Layout {

	final static System.Logger LOG = System.getLogger(Layout.class.getName());

	private final static Map<String, Layout> layouts = new HashMap<>();
	private final static Map<String, String> layoutToName = new HashMap<>();
	private final static Map<String, String> nameToLayout = new HashMap<>();

	static {
		Properties p = new Properties();
		try (InputStream in = Layout.class.getResourceAsStream("languages.properties")) {
			p.load(in);
			for (Object k : p.keySet()) {
				String v = p.getProperty((String) k);
				layoutToName.put((String) k, v);
				nameToLayout.put(v, (String) k);
			}
		} catch (IOException ioe) {
			throw new IllegalStateException("Failed to load language mappings.");
		}
	}

	public static Layout get(String layout, int width, int height) {
		String key = layout + "_" + width;
		if (!layouts.containsKey(key)) {
			URL resource = Layout.class.getResource(width + ".json");
			if (resource == null)
				throw new IllegalStateException(
						String.format("No keyboard layout for language %s and matrix width %d.", layout, width));

			try (InputStream in = resource.openStream()) {
				JsonObject jsonObject = JsonParser.parseReader(new JsonReader(new InputStreamReader(in)))
						.getAsJsonObject();
				JsonElement map = jsonObject.get(layout);
				String layoutName = layout;
				boolean exact = true;
				if (map == null) {
					layoutName = layoutToName.get(layout);
					map = layout == null ? null : jsonObject.get(layoutName);
				} else {
					layout = nameToLayout.get(layoutName);
				}
				if (map == null && !"US".equals(layoutName) && !"en_US".equals(layout)) {
					LOG.log(Level.WARNING, String.format("No exact map for %s, using 'US'", layout));
					layout = "en_US";
					layoutName = "US";
					map = jsonObject.get(layout);
					if (map == null)
						throw new IOException(String.format("Could not find any layout  in %s for keyboard layout %s",
								resource, layout));
					exact = false;
				}

				Layout layoutObj = new Layout();
				layoutObj.setKeys(new MatrixCell[height][width]);
				layoutObj.setLayout(layout);
				layoutObj.setName(layoutName);
				layoutObj.setX(width);
				layoutObj.setY(height);
				layoutObj.setExact(exact);
				layoutObj.setKeys(new MatrixCell[height][]);
				for (int r = 0; r < height; r++) {
					MatrixCell[] rowArr = new MatrixCell[width];
					layoutObj.getKeys()[r] = rowArr;
				}

				JsonObject mapObj = map.getAsJsonObject();

				for (int r = 0; r < height; r++) {
					MatrixCell[] rowArr = new MatrixCell[width];
					layoutObj.getKeys()[r] = rowArr;
					JsonElement row = mapObj.get("row" + r);
					if (row == null)
						LOG.log(Level.WARNING, String.format("No row %d in map %s for layout %s", r, resource, layout));
					JsonArray rowObj = row.getAsJsonArray();
					for (JsonElement el : rowObj) {
						JsonObject elObj = el.getAsJsonObject();
						MatrixCell keyObj = new MatrixCell();
						JsonElement labelEl = elObj.get("label");
						keyObj.setLabel(labelEl.isJsonNull() ? null : labelEl.getAsString());
						keyObj.setDisabled(elObj.has("disabled") ? elObj.get("disabled").getAsBoolean() : false);
						keyObj.setWidth(elObj.has("width") ? elObj.get("width").getAsInt() : 0);
						JsonElement matrix = elObj.get("matrix");
						if (matrix != null) {
							JsonArray arr = matrix.getAsJsonArray();
							keyObj.setMatrixY(arr.get(0).getAsInt());
							keyObj.setMatrixX(arr.get(1).getAsInt());
						}
						layoutObj.getKeys()[(int)keyObj.getMatrixY()][(int)keyObj.getMatrixX()] = keyObj;
					}
				}

				layouts.put(key, layoutObj);
				return layoutObj;

			} catch (IOException ioe) {
				throw new IllegalStateException(
						String.format("Failed to read keyboard layout for language %s and matrix width %d from %s.",
								layout, width, resource));
			}
		}
		return layouts.get(key);
	}

	private boolean exact;
	private MatrixCell[][] keys;
	private String layout;
	private String name;
	private int x;
	private int y;

	public MatrixCell[][] getKeys() {
		return keys;
	}

	public String getLayout() {
		return layout;
	}

	public String getName() {
		return name;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public boolean isExact() {
		return exact;
	}

	public void setExact(boolean exact) {
		this.exact = exact;
	}

	public void setKeys(MatrixCell[][] keys) {
		this.keys = keys;
	}

	public void setLayout(String layout) {
		this.layout = layout;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setX(int x) {
		this.x = x;
	}

	public void setY(int y) {
		this.y = y;
	}

}
