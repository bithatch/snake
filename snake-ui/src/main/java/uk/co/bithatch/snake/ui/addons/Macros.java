package uk.co.bithatch.snake.ui.addons;

import java.io.IOException;
import java.nio.file.Path;

import com.google.gson.JsonObject;

import uk.co.bithatch.macrolib.JsonMacroStorage;
import uk.co.bithatch.macrolib.MacroProfile;
import uk.co.bithatch.snake.ui.App;

public class Macros extends AbstractJsonAddOn {

	final static System.Logger LOG = System.getLogger(Macros.class.getName());

	private MacroProfile profile;

	Macros(Path path, App context) throws IOException {
		super(path, context);
	}

	Macros(JsonObject object, App context) throws IOException {
		super(object, context);
	}

	public Macros(String id) {
		super(id);
	}

	public MacroProfile getProfile() {
		return profile;
	}

	public void setProfile(MacroProfile layout) {
		this.profile = layout;
	}

	@Override
	public String toString() {
		return name;
	}

	@Override
	public void setArchive(Path archive) {
		super.setArchive(archive);
//		if (profile != null)
//			try {
//				profile.setBase(archive.toUri().toURL());
//			} catch (MalformedURLException e) {
//				throw new IllegalStateException("Failed to set base.", e);
//			}
	}

	@Override
	public void close() throws Exception {
//		context.getLayouts().remove(profile);
	}

	@Override
	protected void construct(JsonObject addOnJson) {
		/* Layout */
		JsonObject sequenceJson = addOnJson.get("layout").getAsJsonObject();
		JsonMacroStorage s;
//		profile = new DeviceLayout(null, sequenceJson);
//		if (profile.getName() == null)
//			profile.setName(getName());
	}

	@Override
	protected void store(JsonObject addOnJson) {

//		JsonObject layoutObject = new JsonObject();
//		profile.store(layoutObject);
//		addOnJson.add("layout", layoutObject);
	}

}
