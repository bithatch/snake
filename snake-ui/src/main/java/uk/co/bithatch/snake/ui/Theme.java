package uk.co.bithatch.snake.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.prefs.Preferences;

public class Theme {

	private static Map<String, Theme> themes;
	private final static Preferences PREFS = Preferences.userNodeForPackage(Theme.class);

	public static Theme getActiveTheme() {
		return getTheme(PREFS.get("theme", getThemes().iterator().next().getId()));
	}

	public static void setActiveTheme(String id) {
		PREFS.put("theme", id);
	}

	public static Theme getTheme(String id) {
		if (themes == null)
			getThemes();
		return themes.get(id);
	}

	public static Collection<Theme> getThemes() {
		try {
			if (themes == null) {
				themes = new LinkedHashMap<>();
				for (Enumeration<URL> u = ClassLoader.getSystemResources("META-INF/theme"); u.hasMoreElements();) {
					URL url = u.nextElement();
					try (BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream()))) {
						String line;
						while ((line = r.readLine()) != null) {
							line = line.trim();
							if (!line.startsWith("#") && line.length() > 0) {
								URL tu = ClassLoader.getSystemResource("themes/" + line + "/theme.properties");
								Properties p = new Properties();
								try (InputStream in = tu.openStream()) {
									p.load(in);
								}
								themes.put(line, new Theme(line, p));
							}
						}
					}
				}
			}
		} catch (IOException ioe) {
			throw new IllegalStateException("Failed to load themes.", ioe);
		}
		return themes.values();
	}

	private String name;
	private String url;
	private String license;
	private String description;
	private String author;
	private String id;

	Theme(String id, Properties properties) {
		this.id = id;
		this.name = properties.getProperty("name", id);
		this.url = properties.getProperty("url", "");
		this.license = properties.getProperty("license", "");
		this.description = properties.getProperty("description", "");
		this.author = properties.getProperty("author", "");
	}

	public String getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	@Override
	public String toString() {
		return name;
	}
}
