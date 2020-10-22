package uk.co.bithatch.snake.ui;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

public class AddOnManager {

	public interface Listener {
		void addOnAdded(AddOn addOn);

		void addOnRemoved(AddOn addOn);
	}
	protected static Path getAddOnsDirectory() throws IOException {
		Path addons = Paths.get("addons");
		if (!Files.exists(addons))
			Files.createDirectories(addons);
		return addons;
	}

	private final List<Listener> listeners = new ArrayList<>();

	private Map<String, Theme> themes;

	public void addListener(Listener listener) {
		listeners.add(listener);
	}

	public Theme getTheme(String id) {
		if (themes == null)
			getThemes();
		return themes.get(id);
	}

	public Collection<Theme> getThemes() {
		try {
			if (themes == null) {
				themes = new LinkedHashMap<>();

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

							/* And is it's path a child of the themes directory? */
							if (fileUri.startsWith(getAddOnsDirectory().toUri().toURL().toExternalForm())) {
								Path path = Path.of(new URI(fileUri));
								theme.setArchive(path);
							}
						}
					}
				}
			}
		} catch (Exception ioe) {
			throw new IllegalStateException("Failed to load themes.", ioe);
		}
		return themes.values();
	}

	@SuppressWarnings("unchecked")
	public <T extends AddOn> T install(File file) throws IOException {

		ZipEntry themeEntry = null;
		String addOnName = null;
		try (JarFile jf = new JarFile(file)) {
			themeEntry = jf.getEntry("META-INF/theme");
			if (themeEntry != null) {
				List<String> names = readThemeNames(jf.getInputStream(themeEntry));
				if (!names.isEmpty())
					addOnName = names.get(0);
			}
		}

		if (themeEntry == null)
			throw new IOException(String.format("The file %s does not contain a theme.", file));
		else {
			/* Is a theme */
			Path themes = getThemesDirectory();

			Path targetThemeFile = themes.resolve(addOnName + ".jar");
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

	public void removeListener(Listener listener) {
		listeners.remove(listener);
	}

	public void uninstall(AddOn addOn) throws IOException {
		Files.delete(addOn.getArchive());
		themes.remove(addOn.getId());
		fireRemoved(addOn);
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

	private Path getThemesDirectory() throws IOException {
		Path themes = getAddOnsDirectory().resolve("themes");
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
		themes.put(t, td);
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
