package uk.co.bithatch.snake.ui;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.System.Logger.Level;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import uk.co.bithatch.snake.ui.util.Strings;

public class Cache implements Closeable {
	final static System.Logger LOG = System.getLogger(Cache.class.getName());

	private Properties index = new Properties();
	private Set<URL> waiting = new HashSet<>();
	private Map<String, String> resolved = Collections.synchronizedMap(new HashMap<>());
	private App context;

	public Cache(App context) throws IOException {
		this.context = context;
		File cacheIndexFile = getIndexFile();
		if (cacheIndexFile.exists()) {
			try (InputStream in = new FileInputStream(cacheIndexFile)) {
				index.load(in);
			}
		}
	}

	protected File getIndexFile() {
		return new File(getCacheDir(), "cache.index");
	}

	public String getCachedImage(String image) {
		if (image == null || image.equals(""))
			return null;

		/* Was this previously resolved? */
		String r = resolved.get(image);
		if (r != null)
			return r;

		String hash = Strings.genericHash(image);
		File cacheDir = getCacheDir();
		try {
			File cacheFile = new File(cacheDir, hash);
			if (!cacheFile.exists()) {
				synchronized (index) {

					/*
					 * Let the original URL be returned the first time, so as to not hold up the
					 * current thread, and let JavaFX background image loading happen so the UI
					 * thread is not held up either.
					 * 
					 * In the background, we trigger the downloading of the image which may be used
					 * the next time the URL is request.
					 */
					URL url = new URL(image);
					if (!waiting.contains(url)) {
						waiting.add(url);
						context.getLoadQueue().execute(() -> {
							File tmpCacheFile = new File(cacheDir, hash + ".tmp");
							try (InputStream in = url.openStream()) {
								try (OutputStream out = new FileOutputStream(tmpCacheFile)) {
									in.transferTo(out);
								}
								synchronized (index) {
									/* Index it */
									index.setProperty(image, cacheFile.getName());
									try (OutputStream out = new FileOutputStream(getIndexFile())) {
										index.store(out, "Snake Cache Index");
									} catch (IOException ioe) {
									}
									resolved.put(image, cacheFile.toURI().toURL().toExternalForm());
								}
							} catch (IOException ioe) {
								LOG.log(Level.ERROR, "Failed to cache image.");
								resolved.put(image, image);
								return;
							}
							tmpCacheFile.renameTo(cacheFile);
						});
					}
				}
			} else {
				String path = cacheFile.toURI().toURL().toExternalForm();
				resolved.put(image, path);
				return path;
			}
		} catch (MalformedURLException murle) {
			throw new IllegalStateException(String.format("Failed to construct image URL for %s.", image), murle);
		}
		return image;
	}

	static File getCacheDir() {
		File cacheDir = new File(System.getProperty("user.home") + File.separator + ".cache" + File.separator + "snake"
				+ File.separator + "device-image-cache");
		if (!cacheDir.exists() && !cacheDir.mkdirs()) {
			throw new IllegalStateException(
					String.format("Failed to create device image cache directory %s.", cacheDir));
		}
		return cacheDir;
	}

	@Override
	public void close() throws IOException {
	}
}
