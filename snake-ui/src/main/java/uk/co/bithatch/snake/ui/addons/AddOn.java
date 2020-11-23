package uk.co.bithatch.snake.ui.addons;

import java.net.URL;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

public interface AddOn extends AutoCloseable {

	default boolean hasResources() {
		return !resolveResources(false).isEmpty();
	}

	default Map<String, URL> resolveResources(boolean commit) {
		return Collections.emptyMap();
	}

	URL getLocation();

	String getId();

	String getName();

	String getUrl();

	String getLicense();

	String getDescription();

	String getAuthor();

	URL getScreenshot();

	String[] getSupportedLayouts();

	String[] getUnsupportedLayouts();

	String[] getSupportedModels();

	String[] getUnsupportedModels();

	Path getArchive();

	default boolean isSystem() {
		return getArchive() == null;
	}

	default void uninstall() {
	}

	void setArchive(Path archive);

}