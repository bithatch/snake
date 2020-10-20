package uk.co.bithatch.snake.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.System.Logger.Level;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class Theme {

	final static System.Logger LOG = System.getLogger(Theme.class.getName());

	private static Map<String, Theme> themes;

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
								String tus = tu.toExternalForm();
								int idx = tus.lastIndexOf('/');
								tus = tus.substring(0, idx);
								themes.put(line, new Theme(line, p, new URL(tus)));
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
	private URL location;
	private String parent;

	Theme(String id, Properties properties, URL location) {
		this.id = id;
		this.location = location;
		this.name = properties.getProperty("name", id);
		this.url = properties.getProperty("url", "");
		this.license = properties.getProperty("license", "");
		this.description = properties.getProperty("description", "");
		this.author = properties.getProperty("author", "");
		this.parent = properties.getProperty("parent", "");
	}

	public URL getLocation() {
		return location;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
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

	public URL getResource(String resource) {
		String l = location.toExternalForm();
		try {
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG,
						String.format("Trying to get resource %s as URL in theme %s (%s).", resource, id, l));
			URL r = new URL(l + "/" + resource);
			InputStream in = null;
			try {
				in = r.openStream();
				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG, String.format("Found resource %s in theme %s at %s.", resource, id, r));
				return r;
			} catch (IOException e) {
				if (parent != null && parent.length() > 0) {
					if (LOG.isLoggable(Level.DEBUG))
						LOG.log(Level.DEBUG,
								String.format("%s Doesn't exist in theme %s, trying parent %s.", resource, id, parent));
					return Theme.getTheme(parent).getResource(resource);
				}
			} finally {
				try {
					if (in != null)
						in.close();
				} catch (IOException e) {
				}
			}
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("%s Doesn't exist.", resource));
			return null;

		} catch (MalformedURLException murle) {
			throw new IllegalArgumentException(String.format("Failed to get resource %s.", resource), murle);
		}
	}

	public InputStream getResourceAsStream(String resource) {
		if (LOG.isLoggable(Level.DEBUG))
			LOG.log(Level.DEBUG,
					String.format("Trying to get resource %s as stream in theme %s (%s).", resource, id, location));
		InputStream in = getClass().getResourceAsStream(location.toExternalForm() + "/" + resource);
		if (in == null && parent != null && parent.length() > 0) {
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("Doesn't exist, trying parent %s.", parent));
			in = Theme.getTheme(parent).getResourceAsStream(location.toExternalForm() + "/" + resource);
		}
		return in;
	}
}
