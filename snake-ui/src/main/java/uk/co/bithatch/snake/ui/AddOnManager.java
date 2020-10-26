package uk.co.bithatch.snake.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.System.Logger.Level;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import groovy.util.GroovyScriptEngine;

public class AddOnManager {

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

	/**
	 * Recursively deletes `item`, which may be a directory. Symbolic links will be
	 * deleted instead of their referents. Returns a boolean indicating whether
	 * `item` still exists. http://stackoverflow.com/questions/8666420
	 * 
	 * @param item file to delete
	 * @return deleted OK
	 */
	public static boolean deleteRecursiveIfExists(File item) {
		if (!item.exists())
			return true;
		if (!Files.isSymbolicLink(item.toPath()) && item.isDirectory()) {
			File[] subitems = item.listFiles();
			for (File subitem : subitems)
				if (!deleteRecursiveIfExists(subitem))
					return false;
		}
		return item.delete();
	}

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
					addScriptFile(scriptFile.toPath());
				} catch (Exception e) {
					LOG.log(Level.ERROR, String.format("Failed to load script %s", scriptFile), e);
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

	public AddOn getAddOn(Class<? extends AddOn> clazz, String id) {
		return addOns.get(new AddOnKey(clazz, id));
	}

	public Collection<AddOn> getAddOns() {
		return addOns.values();

	}

	public Collection<Script> getScripts() {
		List<Script> l = new ArrayList<>();
		for (Map.Entry<AddOnKey, AddOn> men : addOns.entrySet()) {
			if (men.getKey().clazz.equals(Script.class)) {
				l.add((Script) men.getValue());
			}
		}
		return l;
	}

	public Theme getTheme(String id) {
		return (Theme) addOns.get(new AddOnKey(Theme.class, id));
	}

	public Collection<Theme> getThemes() {
		List<Theme> l = new ArrayList<>();
		for (Map.Entry<AddOnKey, AddOn> men : addOns.entrySet()) {
			if (men.getKey().clazz.equals(Theme.class)) {
				l.add((Theme) men.getValue());
			}
		}
		return l;
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
			if (fileName.endsWith(".plugin.groovy")) {
				/*
				 * If this is just a single file, then we use file name for the plugin ID, and
				 * create a directory of the same name, and place the script in that.
				 */
				int idx = fileName.indexOf(".plugin.groovy");
				addOnName = fileName.substring(0, idx);

				Path targetScriptDir = getScriptsDirectory().resolve(addOnName);
				if (Files.exists(targetScriptDir))
					throw new Exception(String.format("The theme %s already exists.", addOnName));
				Files.createDirectories(targetScriptDir);
				Path targetThemeFile = targetScriptDir.resolve(addOnName + ".plugin.groovy");
				Files.copy(file.toPath(), targetThemeFile);
				Script scr = addScriptFile(targetScriptDir);
				scr.run();
				scr.install();
				return (T) scr;
			} else {
				throw new IOException(
						String.format("The file %s does not contain a theme, and is not a script.", file));
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

	public void start() {
		for (Script s : getScripts()) {
			try {
				s.run();
			} catch (Exception e) {
				LOG.log(Level.ERROR, "Failed to start script.", e);
			}
		}
	}

	public void uninstall(AddOn addOn) throws IOException {
		deleteRecursiveIfExists(addOn.getArchive().toFile());
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

	private Script addScriptFile(Path scriptDir) throws Exception {
		Script script = new Script(scriptDir, context, groovy);
		addOns.put(new AddOnKey(Script.class, script.getId()), script);
		return script;
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

	private Path getScriptsDirectory() throws IOException {
		Path scripts = getAddOnsDirectory().resolve("scripts");
		if (!Files.exists(scripts))
			Files.createDirectories(scripts);
		return scripts;
	}

	private Path getThemesDirectory() throws IOException {
		Path themes = getAddOnsDirectory().resolve("addOns");
		if (!Files.exists(themes))
			Files.createDirectories(themes);
		return themes;
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
}
