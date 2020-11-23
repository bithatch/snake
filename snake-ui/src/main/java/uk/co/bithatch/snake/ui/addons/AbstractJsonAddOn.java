package uk.co.bithatch.snake.ui.addons;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import uk.co.bithatch.snake.lib.Json;
import uk.co.bithatch.snake.ui.App;

public abstract class AbstractJsonAddOn extends AbstractAddOn {

	final static System.Logger LOG = System.getLogger(AbstractJsonAddOn.class.getName());

	protected App context;

	protected AbstractJsonAddOn(String id) {
		this.id = id;
	}

	protected AbstractJsonAddOn(Path path, App context) throws IOException {
		this(Json.toJson(path).getAsJsonObject(), context);
		archive = path.getParent();
	}

	protected AbstractJsonAddOn(JsonObject addOnJson, App context) throws IOException {

		this.context = context;

		/* Add-on */
		id = addOnJson.get("id").getAsString();
		name = addOnJson.get("name").getAsString();
		description = addOnJson.has("description") ? addOnJson.get("description").getAsString() : null;
		author = addOnJson.has("author") ? addOnJson.get("author").getAsString() : null;
		url = addOnJson.has("url") ? addOnJson.get("url").getAsString() : null;
		license = addOnJson.has("license") ? addOnJson.get("license").getAsString() : null;
		supportedModels = addOnJson.has("supportedModels")
				? Json.toStringArray(addOnJson.get("supportedModels").getAsJsonArray())
				: new String[0];
		unsupportedModels = addOnJson.has("unsupportedModels")
				? Json.toStringArray(addOnJson.get("unsupportedModels").getAsJsonArray())
				: new String[0];
		supportedLayouts = addOnJson.has("supportedLayouts")
				? Json.toStringArray(addOnJson.get("supportedLayouts").getAsJsonArray())
				: new String[0];
		unsupportedLayouts = addOnJson.has("unsupportedLayouts")
				? Json.toStringArray(addOnJson.get("unsupportedLayouts").getAsJsonArray())
				: new String[0];

		String thisAddOn = getClass().getSimpleName().toUpperCase();
		if (!thisAddOn.equals(addOnJson.get("addOnType").getAsString())) {
			throw new IOException(String.format("This add-on instance is a %s, not a %s as specified by the JSON.",
					getClass().getSimpleName(), addOnJson.get("addOnType").getAsString()));
		}

		construct(addOnJson);

	}

	protected abstract void construct(JsonObject addOnJson);

	protected abstract void store(JsonObject addOnJson);

	@SuppressWarnings("resource")
	public final void export(Writer out) {
		JsonObject addOnInfo = new JsonObject();
		addOnInfo.addProperty("id", id);
		addOnInfo.addProperty("name", name);
		addOnInfo.addProperty("addOnType", getClass().getSimpleName().toUpperCase());
		if (description != null && !description.equals(""))
			addOnInfo.addProperty("description", description);
		if (author != null && !author.equals(""))
			addOnInfo.addProperty("author", author);
		if (url != null && !url.equals(""))
			addOnInfo.addProperty("author", url);
		if (license != null && !license.equals(""))
			addOnInfo.addProperty("license", license);
		if (supportedModels.length > 0)
			addOnInfo.add("supportedModels", Json.toStringJsonArray(supportedModels));
		if (unsupportedModels.length > 0)
			addOnInfo.add("unsupportedModels", Json.toStringJsonArray(unsupportedModels));
		if (supportedLayouts.length > 0)
			addOnInfo.add("supportedLayouts", Json.toStringJsonArray(supportedLayouts));
		if (unsupportedLayouts.length > 0)
			addOnInfo.add("unsupportedLayouts", Json.toStringJsonArray(unsupportedLayouts));

		store(addOnInfo);

		GsonBuilder b = new GsonBuilder();
		b.setPrettyPrinting();
		Gson gson = b.create();
		PrintWriter pw = new PrintWriter(out, true);
		pw.println(gson.toJson(addOnInfo));
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public URL getScreenshot() {
		return null;
	}

}
