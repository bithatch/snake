package uk.co.bithatch.snake.ui;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAddOn implements AddOn {
	
	
	protected String name;
	protected String url;
	protected String license;
	protected String description;
	protected String author;
	protected String id;
	protected Path archive;
	protected URL location;

	public AbstractAddOn() {
		super();
	}

	@Override
	public URL getLocation() {
		return location;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Override
	public String getLicense() {
		return license;
	}

	public void setLicense(String license) {
		this.license = license;
	}

	@Override
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@Override
	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public Path getArchive() {
		return archive;
	}

	public void setArchive(Path archive) {
		this.archive = archive;
	}

}