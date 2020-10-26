package uk.co.bithatch.snake.ui;

import java.net.URL;
import java.nio.file.Path;

public interface AddOn extends AutoCloseable {

	URL getLocation();

	String getId();

	String getName();

	String getUrl();

	String getLicense();

	String getDescription();

	String getAuthor();

	URL getScreenshot();

	Path getArchive();

	default boolean isSystem() {
		return getArchive() == null;
	}

}