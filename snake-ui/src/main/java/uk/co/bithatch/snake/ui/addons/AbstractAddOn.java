package uk.co.bithatch.snake.ui.addons;

import java.net.URL;
import java.nio.file.Path;

public abstract class AbstractAddOn implements AddOn {

	protected String name;
	protected String url;
	protected String license;
	protected String description;
	protected String author;
	protected String id;
	protected Path archive;
	protected URL location;
	protected String[] supportedModels = new String[0];
	protected String[] unsupportedModels = new String[0];
	protected String[] supportedLayouts = new String[0];
	protected String[] unsupportedLayouts = new String[0];

	public AbstractAddOn() {
		super();
	}

	public String[] getSupportedModels() {
		return supportedModels;
	}

	public void setSupportedModels(String[] supportedModels) {
		this.supportedModels = supportedModels;
	}

	public String[] getUnsupportedModels() {
		return unsupportedModels;
	}

	public void setUnsupportedModels(String[] unsupportedModels) {
		this.unsupportedModels = unsupportedModels;
	}

	public String[] getSupportedLayouts() {
		return supportedLayouts;
	}

	public void setSupportedLayouts(String[] supportedLayouts) {
		this.supportedLayouts = supportedLayouts;
	}

	public String[] getUnsupportedLayouts() {
		return unsupportedLayouts;
	}

	public void setUnsupportedLayouts(String[] unsupportedLayouts) {
		this.unsupportedLayouts = unsupportedLayouts;
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