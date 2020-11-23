package uk.co.bithatch.snake.ui.addons;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import uk.co.bithatch.snake.lib.Region.Name;
import uk.co.bithatch.snake.lib.layouts.Area;
import uk.co.bithatch.snake.lib.layouts.DeviceLayout;
import uk.co.bithatch.snake.lib.layouts.DeviceView;
import uk.co.bithatch.snake.lib.layouts.IO;
import uk.co.bithatch.snake.lib.layouts.MatrixCell;
import uk.co.bithatch.snake.lib.layouts.MatrixIO;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.util.Strings;

public class Layout extends AbstractJsonAddOn {

	final static System.Logger LOG = System.getLogger(Layout.class.getName());

	private DeviceLayout layout;

	Layout(Path path, App context) throws IOException {
		super(path, context);
	}

	Layout(JsonObject object, App context) throws IOException {
		super(object, context);
	}

	public Layout(String id) {
		super(id);
	}

	public DeviceLayout getLayout() {
		return layout;
	}

	public void setLayout(DeviceLayout layout) {
		this.layout = layout;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public URL getScreenshot() {
		Collection<DeviceView> views = layout.getViews().values();
		String imageUrl = null;
		for (DeviceView view : views) {
			if (view.getImageUri() != null) {
				imageUrl = view.getImageUri();
				break;
			}
		}
		try {
			return imageUrl == null ? null : new URL(imageUrl);
		} catch (MalformedURLException e) {
			if (archive != null) {
				try {
					return archive.resolve(imageUrl).toUri().toURL();
				} catch (MalformedURLException e1) {
				}
			}
		}
		return null;
	}

	@Override
	public Map<String, URL> resolveResources(boolean commit) {
		Map<String, URL> res = new HashMap<>();
		for (DeviceView view : layout.getViews().values()) {
			if (view.getImageUri() != null) {
				try {
					URL url = new URL(view.getImageUri());
					String key = Strings.basename(url.toExternalForm());
					if (key == null) {
						key = "image";
					}
					int idx = 2;
					String okey = key;
					while (res.containsKey(key)) {
						key = okey + idx;
						idx++;
					}
					res.put(key, url);
					if (commit)
						view.setImageUri(key);
				} catch (Exception e) {
				}
			}
		}
		return res;
	}

	@Override
	public void setArchive(Path archive) {
		super.setArchive(archive);
		if (layout != null)
			layout.setBase(archive);
	}

	@Override
	public void close() throws Exception {
		context.getLayouts().remove(layout);
	}

	@Override
	protected void construct(JsonObject addOnJson) {
		/* Layout */
		JsonObject sequenceJson = addOnJson.get("layout").getAsJsonObject();
		layout = new DeviceLayout(archive, sequenceJson);
		if (layout.getName() == null)
			layout.setName(getName());
	}

	@Override
	protected void store(JsonObject addOnJson) {

		JsonObject layoutObject = new JsonObject();
		layoutObject.addProperty("matrixHeight", layout.getMatrixHeight());
		layoutObject.addProperty("matrixWidth", layout.getMatrixWidth());
		layoutObject.addProperty("deviceType", layout.getDeviceType().name());

		JsonArray frameInfo = new JsonArray();
		for (DeviceView deviceView : layout.getViews().values()) {
			JsonObject viewObject = new JsonObject();

			viewObject.addProperty("desaturateImage", deviceView.isDesaturateImage());
			viewObject.addProperty("imageOpacity", deviceView.getImageOpacity());
			if (deviceView.getImageUri() != null)
				viewObject.addProperty("imageUri", deviceView.getImageUri());
			viewObject.addProperty("imageScale", deviceView.getImageScale());
			viewObject.addProperty("position", deviceView.getPosition().name());

			JsonArray elementsArray = new JsonArray();

			for (IO element : deviceView.getElements()) {
				JsonObject elementObject = new JsonObject();
				elementObject.addProperty("type", element.getClass().getSimpleName().toUpperCase());
				elementObject.addProperty("x", element.getX());
				elementObject.addProperty("y", element.getY());
				elementObject.addProperty("label", element.getLabel());
				if (element instanceof Area) {
					Name region = ((Area) element).getRegion();
					if (region != null)
						elementObject.addProperty("region", region.name());
				}
				if (element instanceof MatrixCell) {
					Name region = ((MatrixCell) element).getRegion();
					if (region != null)
						elementObject.addProperty("region", region.name());
				}
				if (element instanceof MatrixIO) {
					elementObject.addProperty("matrixX", ((MatrixIO) element).getMatrixX());
					elementObject.addProperty("matrixY", ((MatrixIO) element).getMatrixY());
				}
				elementsArray.add(elementObject);
			}
			viewObject.add("elements", elementsArray);
			frameInfo.add(viewObject);
		}
		layoutObject.add("views", frameInfo);
		addOnJson.add("layout", layoutObject);
	}

}
