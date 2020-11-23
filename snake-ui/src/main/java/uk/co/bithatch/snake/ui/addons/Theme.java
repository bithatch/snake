package uk.co.bithatch.snake.ui.addons;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import uk.co.bithatch.snake.lib.DeviceType;
import uk.co.bithatch.snake.lib.Region.Name;

public class Theme extends AbstractAddOn {

	final static System.Logger LOG = System.getLogger(Theme.class.getName());

	private String parent;
	private AddOnManager manager;

	Theme(AddOnManager manager, String id, Properties properties, URL location) {
		this.manager = manager;
		this.id = id;
		this.location = location;
		this.name = properties.getProperty("name", id);
		this.url = properties.getProperty("url", "");
		this.license = properties.getProperty("license", "");
		this.description = properties.getProperty("description", "");
		this.author = properties.getProperty("author", "");
		this.parent = properties.getProperty("parent", "");
		this.parent = properties.getProperty("parent", "");
		
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
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
					return manager.getTheme(parent).getResource(resource);
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
		try {
			InputStream in = new URL(location.toExternalForm() + "/" + resource).openStream();
			if (in == null && parent != null && parent.length() > 0) {
				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG, String.format("Doesn't exist, trying parent %s.", parent));
				in = manager.getTheme(parent).getResourceAsStream(location.toExternalForm() + "/" + resource);
			}
			return in;
		} catch (IOException ioe) {
			throw new IllegalStateException(String.format("Failed to load theme resource %s.", resource), ioe);
		}
	}

	@Override
	public URL getScreenshot() {
		return getResource("screenshot.png");
	}

	@Override
	public void close() throws Exception {
	}

	public URL getEffectImage(int size, Class<?> effect) {
		return getEffectImage(size, effect.getSimpleName().toLowerCase());
	}

	public URL getEffectImage(int size, String effect) {
		return checkResource(effect, getResource("effects/" + effect + size + ".png"));
	}

	public URL getDeviceImage(int size, DeviceType type) {
		return checkResource(type, getResource("devices/" + type.name().toLowerCase() + size + ".png"));
	}

	public URL getRegionImage(int size, Name region) {
		return checkResource(region, getResource("regions/" + region.name().toLowerCase() + size + ".png"));
	}

	static URL checkResource(Object ctx, URL url) {
		if (url == null)
			throw new IllegalArgumentException(String.format("Image for %s does not exist.", String.valueOf(ctx)));
		return url;
	}

}
