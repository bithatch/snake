package uk.co.bithatch.snake.ui.addons;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.System.Logger.Level;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import com.google.gson.JsonObject;

import groovy.util.GroovyScriptEngine;
import uk.co.bithatch.macrolib.MacroProfile;
import uk.co.bithatch.snake.lib.Backend.BackendListener;
import uk.co.bithatch.snake.lib.animation.Sequence;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Json;
import uk.co.bithatch.snake.lib.layouts.DeviceLayout;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.Controller;
import uk.co.bithatch.snake.ui.DynamicClassLoader;
import uk.co.bithatch.snake.ui.effects.CustomEffectHandler;
import uk.co.bithatch.snake.ui.util.Filing;

public class AddOnManager implements BackendListener {

	public interface Listener {
		void addOnAdded(AddOn addOn);

		void addOnRemoved(AddOn addOn);
	}

	static class AddOnKey {
		Class<? extends AddOn> clazz;
		String id;

		public AddOnKey(Class<? extends AddOn> clazz, String id) {
			super();
			this.clazz = clazz;
			this.id = id;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			AddOnKey other = (AddOnKey) obj;
			if (clazz == null) {
				if (other.clazz != null)
					return false;
			} else if (!clazz.equals(other.clazz))
				return false;
			if (id == null) {
				if (other.id != null)
					return false;
			} else if (!id.equals(other.id))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
			result = prime * result + ((id == null) ? 0 : id.hashCode());
			return result;
		}

	}

	final static System.Logger LOG = System.getLogger(AddOnManager.class.getName());

	protected static Path getAddOnsDirectory() throws IOException {
		Path addons = Paths.get("addons");
		if (!Files.exists(addons))
			Files.createDirectories(addons);
		return addons;
	}

	private Map<AddOnKey, AddOn> addOns;
	private App context;
	private GroovyScriptEngine groovy;

	private final List<Listener> listeners = new ArrayList<>();

	public AddOnManager(App context) {
		this.context = context;
		addOns = new LinkedHashMap<>();

		try {
			Path scriptsDirectory = getScriptsDirectory();
			groovy = new GroovyScriptEngine(new URL[] { scriptsDirectory.toUri().toURL() });
			addOns = new LinkedHashMap<>();
			for (File scriptFile : scriptsDirectory.toFile()
					.listFiles((p) -> p.isDirectory() && new File(p, p.getName() + ".plugin.groovy").exists())) {
				try {
					Script script = new Script(scriptFile.toPath(), context, groovy);
					addOns.put(new AddOnKey(Script.class, script.getId()), script);
				} catch (Exception e) {
					LOG.log(Level.ERROR, String.format("Failed to load script %s", scriptFile), e);
				}
			}
		} catch (Exception e) {
			LOG.log(Level.ERROR, "Failed to load scripts", e);
		}

		try {
			Path effectsDirectory = getEffectsDirectory();
			for (File effectFile : effectsDirectory.toFile()
					.listFiles((p) -> p.isDirectory() && new File(p, p.getName() + ".json").exists())) {
				try {
					AddOn addOn = new CustomEffect(effectFile.toPath().resolve(effectFile.getName() + ".json"),
							context);
					addOns.put(new AddOnKey(addOn.getClass(), addOn.getId()), addOn);
				} catch (Exception e) {
					LOG.log(Level.ERROR, String.format("Failed to load effect %s", effectFile), e);
				}
			}
		} catch (Exception e) {
			LOG.log(Level.ERROR, "Failed to load scripts", e);
		}

		try {
			Path layoutsDirectory = getLayoutsDirectory();
			for (File layoutFile : layoutsDirectory.toFile()
					.listFiles((p) -> p.isDirectory() && new File(p, p.getName() + ".json").exists())) {
				try {
					Layout addOn = new Layout(layoutFile.toPath().resolve(layoutFile.getName() + ".json"), context);
					addOns.put(new AddOnKey(addOn.getClass(), addOn.getId()), addOn);
					DeviceLayout layout = addOn.getLayout();
					layout.setBase(addOn.getArchive().toUri().toURL());
					layout.setReadOnly(true);
					context.getLayouts().addLayout(layout);
				} catch (Exception e) {
					LOG.log(Level.ERROR, String.format("Failed to load layout%s", layoutFile), e);
				}
			}
		} catch (Exception e) {
			LOG.log(Level.ERROR, "Failed to load scripts", e);
		}

		try {
			Path macrosDirectory = getMacrosDirectory();
			for (File layoutFile : macrosDirectory.toFile()
					.listFiles((p) -> p.isDirectory() && new File(p, p.getName() + ".json").exists())) {
				try {
					Macros addOn = new Macros(layoutFile.toPath().resolve(layoutFile.getName() + ".json"), context);
					addOns.put(new AddOnKey(addOn.getClass(), addOn.getId()), addOn);
					MacroProfile macroProfile = addOn.getProfile();
//					macroProfile.setBase(addOn.getArchive().toUri().toURL());
//					addOn.setReadOnly(true);
//					context.getLayouts().addLayout(macroProfile);
				} catch (Exception e) {
					LOG.log(Level.ERROR, String.format("Failed to load layout%s", layoutFile), e);
				}
			}
		} catch (Exception e) {
			LOG.log(Level.ERROR, "Failed to load scripts", e);
		}

		try {
			if (ClassLoader.getSystemClassLoader() instanceof DynamicClassLoader) {
				DynamicClassLoader dlc = (DynamicClassLoader) ClassLoader.getSystemClassLoader();
				for (File themeFile : getThemesDirectory().toFile().listFiles()) {
					dlc.add(themeFile.toURI().toURL());
				}
			}

//				for (File themeFile : getThemesDirectory().listFiles()) {
//					try {
//						// Curent ModuleLayer is usually boot layer. but it can be different if you are
//						// using multiple layers
//						//ModuleLayer currentModuleLayer = Theme.class.getModule().getLayer(); 
//						 ModuleLayer currentModuleLayer = ModuleLayer.boot();
//						final Set<Path> modulePathSet = Set.of(themeFile.toPath());
//						// ModuleFinder to find modules
//						final ModuleFinder moduleFinder = ModuleFinder.of(modulePatThemehSet.toArray(new Path[0]));
//						// I really dont know why does it requires empty finder.
//						final ModuleFinder emptyFinder = ModuleFinder.of(new Path[0]);
//						// ModuleNames to be loaded
//						final Set<String> moduleNames = moduleFinder.findAll().stream()
//								.map(moduleRef -> moduleRef.descriptor().name()).collect(Collectors.toSet());
//						// Unless you want to use URLClassloader for tomcat like situation, use Current
//						// Class Loader
//						final ClassLoader loader = Theme.class.getClassLoader();
//						// Derive new configuration from current module layer configuration
//						final java.lang.module.Configuration configuration = currentModuleLayer.configuration()
//								.resolveAndBind(moduleFinder, emptyFinder, moduleNames);
//						// New Module layer derived from current modulee layer
//						final ModuleLayer moduleLayer = currentModuleLayer.defineModulesWithOneLoader(configuration,
//								loader);
//						// find module and load class Load class
//						final Class<?> controllerClass = moduleLayer.findModule("org.util.npci.coreconnect").get()
//								.getClassLoader().loadClass("org.util.npci.coreconnect.CoreController");
//
//					} catch (Exception e) {
//						LOG.log(Level.ERROR, String.format("Failed to load theme add-on %s.", themeFile), e);
//					}
//				}

			for (Enumeration<URL> u = ClassLoader.getSystemResources("META-INF/theme"); u.hasMoreElements();) {
				URL url = u.nextElement();
				InputStream themeIn = url.openStream();
				for (String t : readThemeNames(themeIn)) {
					Theme theme = loadTheme(t);

					if (url.toExternalForm().startsWith("jar:file")) {
						/* Is this theme in a jar file (not directly on classpath) */
						String fileUri = url.toExternalForm().substring(4);
						int idx = fileUri.indexOf("!");
						if (idx != -1)
							fileUri = fileUri.substring(0, idx);

						/* And is it's path a child of the addOns directory? */
						if (fileUri.startsWith(getAddOnsDirectory().toUri().toURL().toExternalForm())) {
							Path path = Path.of(new URI(fileUri));
							theme.setArchive(path);
						}
					}
				}
			}
		} catch (Exception ioe) {
			throw new IllegalStateException("Failed to load themes.", ioe);
		}
	}

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	@SuppressWarnings("unchecked")
	public <A extends AddOn> A getAddOn(Class<? extends AddOn> clazz, String id) {
		return (A) addOns.get(new AddOnKey(clazz, id));
	}

	public Collection<AddOn> getAddOns() {
		return addOns.values();

	}

	public Collection<Script> getScripts() {
		return getAddOns(Script.class);
	}

	public Collection<CustomEffect> getCustomEffects() {
		return getAddOns(CustomEffect.class);
	}

	public Collection<Theme> getThemes() {
		return getAddOns(Theme.class);
	}

	public Collection<Layout> getLayouts() {
		return getAddOns(Layout.class);
	}

	@SuppressWarnings("unchecked")
	public <A extends AddOn> Collection<A> getAddOns(Class<A> clazz) {
		List<A> l = new ArrayList<>();
		for (Map.Entry<AddOnKey, AddOn> men : addOns.entrySet()) {
			if (men.getKey().clazz.equals(clazz)) {
				l.add((A) men.getValue());
			}
		}
		return l;
	}

	public Theme getTheme(String id) {
		return getAddOn(Theme.class, id);
	}

	public Script getScript(String id) {
		return getAddOn(Script.class, id);
	}

	public Layout getLayout(String id) {
		return getAddOn(Script.class, id);
	}

	public CustomEffect getCustomEffect(String id) {
		return getAddOn(CustomEffect.class, id);
	}

	@SuppressWarnings("unchecked")
	public <T extends AddOn> T install(File file) throws Exception {

		ZipEntry themeEntry = null;
		String addOnName = null;
		String fileName = file.getName();

		if (fileName.endsWith(".jar")) {
			try (JarFile jf = new JarFile(file)) {
				themeEntry = jf.getEntry("META-INF/theme");
				if (themeEntry != null) {
					List<String> names = readThemeNames(jf.getInputStream(themeEntry));
					if (!names.isEmpty())
						addOnName = names.get(0);
				}
			}
		}

		if (themeEntry == null) {
			InstallSession<T> session = new InstallSession<>();
			if (fileName.endsWith(".zip")) {
				Path tmpDir = Files.createTempDirectory("addon");
				Filing.unzip(file.toPath(), tmpDir);
				session.install(tmpDir);
				try {
					return session.commit();
				} finally {
					Filing.deleteRecursiveIfExists(tmpDir.toFile());
				}
			} else {
				session.install(file.toPath());
				return session.commit();
			}
		} else {
			/* Is a theme */
			Path themes = getThemesDirectory();

			Path targetThemeFile = themes.resolve(addOnName + ".jar");
			if (Files.exists(targetThemeFile))
				throw new Exception(String.format("The theme %s already exists.", addOnName));
			Files.copy(file.toPath(), targetThemeFile);

			if (ClassLoader.getSystemClassLoader() instanceof DynamicClassLoader) {
				DynamicClassLoader dlc = (DynamicClassLoader) ClassLoader.getSystemClassLoader();
				dlc.add(targetThemeFile.toUri().toURL());
				T theme = (T) loadTheme(addOnName);
				((Theme) theme).setArchive(targetThemeFile);
				fireAdded(theme);
				return theme;
			} else
				// TODO treat this is restart needed
				return null;
		}
	}

	public void pop(Controller controller) {
		invoke("pop", controller);
		invoke("pop" + controller.getClass().getSimpleName(), controller);
	}

	public void push(Controller controller) {
		invoke("push", controller);
		invoke("push" + controller.getClass().getSimpleName(), controller);
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public Path getMacrosDirectory() throws IOException {
		return getAddOnDirectory("macros");
	}

	public void start() {
		for (CustomEffect effect : getCustomEffects()) {
			try {
				startDeviceEffects(effect);
			} catch (Exception e) {
				LOG.log(Level.ERROR, "Failed to start script.", e);
			}
		}
		context.getBackend().addListener(this);

		for (Script s : getScripts()) {
			try {
				s.run();
			} catch (Exception e) {
				LOG.log(Level.ERROR, "Failed to start script.", e);
			}
		}
	}

	public void uninstall(AddOn addOn) throws Exception {
		addOn.close();
		Filing.deleteRecursiveIfExists(addOn.getArchive().toFile());
		addOns.remove(new AddOnKey(addOn.getClass(), addOn.getId()));
		fireRemoved(addOn);
	}

	protected void invoke(String method, Object... args) {
		for (Script s : getScripts()) {
			try {
				s.invoke(method, args);
			} catch (Exception e) {
				LOG.log(Level.ERROR, String.format("Failed to invoke %s on script %s.", method, s.getArchive()), e);
			}
		}
	}

//	private Layout createLayoutFile(Path jsonFile) throws Exception {
//		Layout layout = new Layout(jsonFile, context);
//		layout.getLayout().setReadOnly(true);
//		layout.setArchive(jsonFile);
//		addOns.put(new AddOnKey(Layout.class, layout.getId()), layout);
//		context.getLayouts().addLayout(layout.getLayout());
//		return layout;
//	}
//
//	private CustomEffect createEffectFile(Path jsonFile) throws Exception {
//		CustomEffect effect = new CustomEffect(jsonFile, context);
//		effect.setArchive(jsonFile);
//		return effect;
//	}
//
//	private CustomEffect createEffectFile(JsonObject object) throws Exception {
//		return new CustomEffect(object, context);
//	}

	protected void startDeviceEffects(CustomEffect effect) throws Exception {
		for (Device dev : context.getBackend().getDevices()) {
			startDeviceEffects(effect, dev);
		}
	}

	protected void startDeviceEffects(CustomEffect effect, Device dev) {
		CustomEffectHandler handler = new CustomEffectHandler(effect.getName(), new Sequence(effect.getSequence()));
		handler.setReadOnly(true);
		context.getEffectManager().add(dev, handler);
		effect.getHandlers().add(handler);
	}

	private <T extends AddOn> void fireAdded(T theme) {
		for (int i = listeners.size() - 1; i >= 0; i--) {
			listeners.get(i).addOnAdded(theme);
		}
	}

	private <T extends AddOn> void fireRemoved(T theme) {
		for (int i = listeners.size() - 1; i >= 0; i--) {
			listeners.get(i).addOnRemoved(theme);
		}
	}

	private Path getAddOnDirectory(String type) throws IOException {
		Path addOns = getAddOnsDirectory().resolve(type);
		if (!Files.exists(addOns))
			Files.createDirectories(addOns);
		return addOns;
	}

	private Path getScriptsDirectory() throws IOException {
		return getAddOnDirectory("scripts");
	}

	private Path getEffectsDirectory() throws IOException {
		return getAddOnDirectory("effects");
	}

	private Path getThemesDirectory() throws IOException {
		return getAddOnDirectory("themes");
	}

	private Path getLayoutsDirectory() throws IOException {
		return getAddOnDirectory("layouts");
	}

	private Theme loadTheme(String t) throws IOException, MalformedURLException {
		String defpath = "themes/" + t + "/theme.properties";
		URL tu = ClassLoader.getSystemResource(defpath);
		if (tu == null)
			throw new IOException(
					String.format("Theme definition for %s does not contain a resource named %s.", t, defpath));
		Theme td = readThemeDef(t, tu);
		addOns.put(new AddOnKey(Theme.class, t), td);
		return td;
	}

	private Theme readThemeDef(String ti, URL def) throws IOException, MalformedURLException {
		Properties p = new Properties();
		try (InputStream in = def.openStream()) {
			p.load(in);
		}
		String tus = def.toExternalForm();
		int idx = tus.lastIndexOf('/');
		tus = tus.substring(0, idx);
		return new Theme(this, ti, p, new URL(tus));
	}

	private List<String> readThemeNames(InputStream themeIn) throws IOException, MalformedURLException {
		List<String> themes = new ArrayList<>();
		try (BufferedReader r = new BufferedReader(new InputStreamReader(themeIn))) {
			String line;
			while ((line = r.readLine()) != null) {
				line = line.trim();
				if (!line.startsWith("#") && line.length() > 0) {
					themes.add(line);
				}
			}
		}
		return themes;
	}

	class InstallSession<A extends AddOn> {
		String addOnName;
		A addOn = null;
		Path targetDir;
		Path targetFile;
		Path sourceFile;
		Path sourceDir;
		List<Path> resources = new ArrayList<>();

		void install(Path file) throws Exception {
			if (Files.isDirectory(file)) {

				/*
				 * If the path is a directory, look for something that looks like a plugin or an
				 * add-on metadata file
				 */
				sourceDir = file;
				try (DirectoryStream<Path> stream = Files.newDirectoryStream(file)) {
					for (Path path : stream) {
						if (!Files.isDirectory(path)) {
							try {
								installFile(path);
								sourceFile = path;
							} catch (IllegalArgumentException iae) {
							}
						}
					}
				}
				if (sourceFile != null) {
					Files.walkFileTree(file, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path innerFile, BasicFileAttributes attrs) throws IOException {
							if (!Files.isDirectory(innerFile) && !innerFile.equals(sourceFile)) {
								resources.add(file.relativize(innerFile));
							}
							return FileVisitResult.CONTINUE;
						}
					});
				} else
					throw new IOException(String
							.format("The file %s does not contain a theme, and is not a script or JSON add-on.", file));
			} else {
				/* Otherwise its just a meta-data file of some sort */
				sourceFile = file;
				installFile(file);
			}
		}

		@SuppressWarnings("unchecked")
		void installFile(Path file) throws Exception {
			String fileName = file.getFileName().toString();
			if (fileName.endsWith(".json")) {
				JsonObject toJson = Json.toJson(file).getAsJsonObject();
				if (toJson.has("addOnType")) {

					/*
					 * Parse the JSON from the source file first, then use the ID embedded within to
					 * determine the add-on id, and thus its actual installed location
					 */

					String type = toJson.get("addOnType").getAsString();
					if (type.equals("CUSTOMEFFECT")) {
						addOn = (A) new CustomEffect(toJson, context);
						targetDir = getEffectsDirectory().resolve(addOn.getId());
					} else if (type.equals("LAYOUT")) {
						addOn = (A) new Layout(toJson, context);
						targetDir = getLayoutsDirectory().resolve(addOn.getId());
					} else
						throw new IOException(String.format("Unknown add-on type %s", type));

					addOnName = addOn.getId();
					targetFile = targetDir.resolve(addOnName + ".json");
				} else {
					throw new IllegalArgumentException(
							"Could not determine add-on type. The JSON had no addOnType attribute.");
				}
			} else if (fileName.endsWith(".plugin.groovy")) {
				/*
				 * If this is just a single file, then we use file name for the plugin ID, and
				 * create a directory of the same name, and place the script in that.
				 */
				int idx = fileName.indexOf(".plugin.groovy");
				addOnName = fileName.substring(0, idx);

				targetDir = getScriptsDirectory().resolve(addOnName);
				targetFile = targetDir.resolve(addOnName + ".plugin.groovy");

				addOn = (A) new Script(file, context, groovy);
			} else {
				throw new IllegalArgumentException(String
						.format("The file %s does not contain a theme, and is not a script or JSON add-on.", file));
			}
		}

		public A commit() throws Exception {

			if (Files.exists(targetDir)) {
				Filing.deleteRecursiveIfExists(targetDir.toFile());
			}
			Files.createDirectories(targetDir);

			/* Copy main artifact */
			Files.copy(sourceFile, targetFile);
			addOn.setArchive(targetDir);

			/* Copy other artifacts */
			if (sourceDir != null) {
				for (Path resource : resources) {
					Files.copy(sourceDir.resolve(resource), targetDir.resolve(resource));
				}
			}
			addOns.put(new AddOnKey(addOn.getClass(), addOn.getId()), addOn);

			fireAdded(addOn);
			if (addOn instanceof CustomEffect) {
				startDeviceEffects((CustomEffect) addOn);
			} else if (addOn instanceof Script) {
				((Script) addOn).run();
				((Script) addOn).install();
			} else if (addOn instanceof Layout) {
				DeviceLayout layout = ((Layout) addOn).getLayout();
				layout.setBase(addOn.getArchive().toUri().toURL());
				layout.setReadOnly(true);
				context.getLayouts().addLayout(layout);
			}

			return (A) addOn;
		}
	}

	@Override
	public void deviceAdded(Device device) {
		for (CustomEffect effect : getCustomEffects()) {
			try {
				startDeviceEffects(effect, device);
			} catch (Exception e) {
				LOG.log(Level.ERROR, "Failed to start script.", e);
			}
		}
	}

	@Override
	public void deviceRemoved(Device device) {
	}
}
