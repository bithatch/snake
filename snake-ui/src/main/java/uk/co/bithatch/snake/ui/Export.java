package uk.co.bithatch.snake.ui;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import uk.co.bithatch.snake.ui.addons.AbstractAddOn;
import uk.co.bithatch.snake.ui.addons.AbstractJsonAddOn;
import uk.co.bithatch.snake.ui.addons.AddOn;
import uk.co.bithatch.snake.ui.util.JavaFX;
import uk.co.bithatch.snake.ui.util.Strings;

public class Export extends AbstractController implements Modal {

	public enum OutputType {
		METADATA_ONLY, BUNDLE
	}

	public interface Validator {
		boolean validate(Export export);
	}

	final static ResourceBundle bundle = ResourceBundle.getBundle(Export.class.getName());

	private static final String PREF_AUTHOR = "author";
	private static final String PREF_LICENSE = "license";
	private static final String PREF_URL = "url";

	private AddOn addOn;

	@FXML
	private TextArea addOnDescription;

	@FXML
	private Label addOnId;

	@FXML
	private TextField addOnName;

	@FXML
	private Label addOnType;

	@FXML
	private TextField author;

	@FXML
	private Hyperlink cancel;

	@FXML
	private Hyperlink confirm;

	@FXML
	private Label description;

	@FXML
	private TextField license;

	@FXML
	private TextField output;
	@FXML
	private Label title;
	@FXML
	private TextField url;
	@FXML
	private ComboBox<OutputType> outputType;

	private Preferences prefs;
	private Validator validator;
	private ResourceBundle exporterBundle;
	private String prefix;

	public void validationError(String key, Object... args) {
		String text = bundle.getString(key);
		if (args.length > 0)
			text = MessageFormat.format(text, (Object[]) args);
		notifyMessage(MessageType.DANGER, bundle.getString("error.validation"), text);
		confirm.disableProperty().set(true);
	}

	public void export(AddOn addOn, ResourceBundle exporterBundle, String prefix, String... args) {
		this.addOn = addOn;

		outputType.itemsProperty().get().addAll(Arrays.asList(OutputType.values()));
		if (addOn.hasResources()) {
			outputType.getSelectionModel().select(OutputType.BUNDLE);
			for (Map.Entry<String, URL> en : addOn.resolveResources(false).entrySet()) {
				if (en.getValue().getProtocol().equals("file")) {
					outputType.disableProperty().set(true);
					break;
				}
			}
		} else {
			outputType.disableProperty().set(true);
			outputType.getSelectionModel().select(OutputType.METADATA_ONLY);
		}

		title.textProperty().set(MessageFormat.format(exporterBundle.getString(prefix + ".title"), (Object[]) args));
		description.textProperty()
				.set(MessageFormat.format(exporterBundle.getString(prefix + ".description"), (Object[]) args));
		confirm.textProperty()
				.set(MessageFormat.format(exporterBundle.getString(prefix + ".confirm"), (Object[]) args));
		cancel.textProperty().set(MessageFormat.format(exporterBundle.getString(prefix + ".cancel"), (Object[]) args));

		this.prefix = prefix;
		this.exporterBundle = exporterBundle;

		addOnName.setText(addOn.getName());
		addOnName.textProperty().addListener((e) -> validateInput());

		addOnType.setText(AddOns.bundle.getString("addOnType." + addOn.getClass().getSimpleName()));
		addOnId.setText(addOn.getId());

		addOnDescription.setText(addOn.getDescription());
		addOnDescription.textProperty().addListener((e) -> validateInput());

		license.setText(Strings.defaultIfBlank(addOn.getLicense(), prefs.get(PREF_LICENSE, "GPLv3")));
		license.promptTextProperty().set("GPLv3");
		license.textProperty().addListener((e) -> validateInput());

		author.setText(Strings.defaultIfBlank(addOn.getAuthor(), prefs.get(PREF_AUTHOR, "")));
		author.promptTextProperty().set(System.getProperty("user.name"));
		author.textProperty().addListener((e) -> validateInput());

		String defaultOutputLocation = getDefaultOutputLocation();
		String ext = Strings.extension(defaultOutputLocation);
		if ("zip".equalsIgnoreCase(ext)
				&& !outputType.getSelectionModel().getSelectedItem().equals(OutputType.BUNDLE)) {
			defaultOutputLocation = Strings.changeExtension(defaultOutputLocation, "json");
		} else if (!"zip".equalsIgnoreCase(ext)
				&& outputType.getSelectionModel().getSelectedItem().equals(OutputType.BUNDLE)) {
			defaultOutputLocation = Strings.changeExtension(defaultOutputLocation, "zip");
		}
		output.textProperty().set(defaultOutputLocation);
		output.textProperty().addListener((e) -> validateInput());

		url.setText(Strings.defaultIfBlank(addOn.getUrl(), prefs.get(PREF_URL, "")));
		url.textProperty().addListener((e) -> validateInput());

		validateInput();

	}

	public TextArea getAddOnDescription() {
		return addOnDescription;
	}

	public Label getAddOnId() {
		return addOnId;
	}

	public TextField getAddOnName() {
		return addOnName;
	}

	public Label getAddOnType() {
		return addOnType;
	}

	public TextField getAuthor() {
		return author;
	}

	public TextField getLicense() {
		return license;
	}

	public TextField getOutput() {
		return output;
	}

	public TextField getUrl() {
		return url;
	}

	public Validator getValidator() {
		return validator;
	}

	public void setValidator(Validator validator) {
		this.validator = validator;
	}

	protected void clearErrors() {
		clearNotifications(false);
		confirm.disableProperty().set(false);
	}

	protected String getDefaultOutputLocation() {
		String id = addOnId.textProperty().get();
		var path = prefs.get(prefix + ".lastExportLocation", System.getProperty("user.dir")) + File.separator + id
				+ (outputType.getSelectionModel().getSelectedItem().equals(OutputType.BUNDLE) ? ".zip" : ".json");
		return path;
	}

	@Override
	protected void onConfigure() throws Exception {
		prefs = context.getPreferences().node("export");
	}

	protected void validateInput() {
		if (addOnName.textProperty().get().equals("")) {
			validationError("error.noAddOnName");
		} else if (addOnDescription.textProperty().get().equals("")) {
			validationError("error.noAddOnDescription");
		} else if (output.textProperty().get().equals("")) {
			validationError("error.noOutput");
		} else if (outputType.getSelectionModel().getSelectedItem().equals(OutputType.BUNDLE)
				&& !output.textProperty().get().toLowerCase().endsWith(".zip")
				&& !output.textProperty().get().toLowerCase().endsWith(".jar")) {
			validationError("error.notZip");
		} else {
			boolean ok = true;
			if (validator != null && !validator.validate(this))
				ok = false;
			if (ok)
				clearErrors();
		}
	}

	void confirm() {

		AbstractAddOn aaddOn = (AbstractAddOn) addOn;
		aaddOn.setName(addOnName.textProperty().get());
		aaddOn.setDescription(addOnDescription.textProperty().get());
		aaddOn.setLicense(Strings.defaultIfBlank(license.textProperty().get(), "GPLv3"));
		aaddOn.setAuthor(Strings.defaultIfBlank(author.textProperty().get(), System.getProperty("user.name")));
		aaddOn.setUrl(url.textProperty().get());

		prefs.put(PREF_LICENSE, aaddOn.getLicense());
		prefs.put(PREF_AUTHOR, aaddOn.getAuthor());
		prefs.put(PREF_URL, aaddOn.getUrl());

		String outPath = output.textProperty().get();

		getScene().getRoot().disableProperty().set(true);
		prefs.put(prefix + ".lastExportLocation", new File(outPath).getParentFile().getAbsolutePath());

		context.getLoadQueue().execute(() -> {

			try (OutputStream w = new BufferedOutputStream(new FileOutputStream(outPath))) {
				if (outputType.getSelectionModel().getSelectedItem().equals(OutputType.BUNDLE)) {
					try (ZipOutputStream zos = new ZipOutputStream(w)) {
						for (Map.Entry<String, URL> en : aaddOn.resolveResources(true).entrySet()) {
							ZipEntry entry = new ZipEntry(en.getKey());
							zos.putNextEntry(entry);
							try (InputStream in = en.getValue().openStream()) {
								in.transferTo(zos);
							}
							zos.closeEntry();
						}
						ZipEntry entry = new ZipEntry(Strings.basename(Strings.changeExtension(outPath, "json")));
						zos.putNextEntry(entry);
						((AbstractJsonAddOn) aaddOn).export(new OutputStreamWriter(zos));
						zos.closeEntry();
					}
				} else {
					((AbstractJsonAddOn) aaddOn).export(new OutputStreamWriter(w));
				}

				Platform.runLater(() -> {
					context.pop();
					context.peek().notifyMessage(MessageType.SUCCESS, bundle.getString("success.export"),
							MessageFormat.format(bundle.getString("success.export.content"), addOn.getName(), outPath));
				});
			} catch (IOException ioe) {
				LOG.log(Level.ERROR, "Failed to export.", ioe);
				Platform.runLater(() -> {
					getScene().getRoot().disableProperty().set(false);
					context.peek().notifyMessage(MessageType.SUCCESS, bundle.getString("error.export"),
							MessageFormat.format(bundle.getString("error.export.content"), addOn.getName(), outPath,
									ioe.getLocalizedMessage()));
				});
			}

		});

	}

	@FXML
	void evtBrowse() {

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(exporterBundle.getString(prefix + ".selectExportFile"));
		String outputPath = output.textProperty().get();
		var path = Strings.defaultIfBlank(outputPath, getDefaultOutputLocation());
		if (outputType.getSelectionModel().getSelectedItem().equals(OutputType.BUNDLE)) {
			fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("addOnBundle"), "*.zip"));
		} else {
			fileChooser.getExtensionFilters()
					.add(new ExtensionFilter(exporterBundle.getString(prefix + ".fileExtension"), "*.json"));
		}

		/* Check extension is right for type */
		path = getActualPath(path);

		fileChooser.getExtensionFilters()
				.add(new ExtensionFilter(exporterBundle.getString(prefix + ".allFiles"), "*.*"));
		JavaFX.selectFilesDir(fileChooser, path);
		File file = fileChooser.showSaveDialog(getScene().getWindow());
		if (file != null) {
			prefs.put(prefix + ".lastExportLocation", file.getParentFile().getAbsolutePath());
			output.textProperty().set(file.getAbsolutePath());
		}
	}

	protected String getActualPath(String path) {
		String ext = Strings.extension(path);
		boolean bundled = outputType.getSelectionModel().getSelectedItem().equals(OutputType.BUNDLE);
		if ("zip".equalsIgnoreCase(ext) && !bundled) {
			path = Strings.changeExtension(path, "json");
		} else if (!"zip".equalsIgnoreCase(ext) && bundled) {
			path = Strings.changeExtension(path, "zip");
		}
		return path;
	}

	@FXML
	void evtOutputType() {
		output.textProperty().set(getActualPath(output.textProperty().get()));
	}

	@FXML
	void evtCancel() {
		context.pop();
	}

	@FXML
	void evtConfirm() {
		confirm();
	}
}
