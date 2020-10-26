package uk.co.bithatch.snake.ui;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import org.apache.commons.lang3.StringUtils;

import javafx.animation.FadeTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Duration;
import uk.co.bithatch.snake.ui.SlideyStack.Direction;

public class AddOns extends AbstractController {
	final static Preferences PREFS = Preferences.userNodeForPackage(AddOns.class);

	final static ResourceBundle bundle = ResourceBundle.getBundle(AddOns.class.getName());

	@FXML
	private Hyperlink add;
	@FXML
	private Hyperlink url;
	@FXML
	private Label by;
	@FXML
	private Label license;
	@FXML
	private Label addOnType;
	@FXML
	private Label addOnName;
	@FXML
	private Label description;
	@FXML
	private VBox addOnDetailsContainer;
	@FXML
	private ListView<Node> installed;
	@FXML
	private Label error;
	@FXML
	private Label systemAddOn;
	@FXML
	private Hyperlink deleteAddOn;

	private Map<Node, AddOn> addOnMap = new HashMap<>();

	@Override
	protected void onCleanUp() {
	}

	@Override
	protected void onConfigure() throws Exception {
		deleteAddOn.managedProperty().bind(deleteAddOn.visibleProperty());
		systemAddOn.managedProperty().bind(systemAddOn.visibleProperty());
		addOnName.managedProperty().bind(addOnName.visibleProperty());
		addOnType.managedProperty().bind(addOnType.visibleProperty());
		by.managedProperty().bind(by.visibleProperty());
		url.managedProperty().bind(url.visibleProperty());
		description.managedProperty().bind(description.visibleProperty());
		license.managedProperty().bind(license.visibleProperty());
		addOnDetailsContainer.managedProperty().bind(addOnDetailsContainer.visibleProperty());

		installed.getSelectionModel().selectedItemProperty().addListener((e) -> updateSelected());
		installed.getItems().clear();
		for (AddOn addOn : context.getAddOnManager().getAddOns()) {
			addAddOn(addOn);
		}
		if (!installed.getItems().isEmpty()) {
			installed.getSelectionModel().select(0);
		}
		updateSelected();
	}

	private AddOnDetails addAddOn(AddOn addOn) throws IOException {
		AddOnDetails aod = context.openScene(AddOnDetails.class);
		aod.setAddOn(addOn);
		Parent node = aod.getScene().getRoot();
		installed.getItems().add(node);
		addOnMap.put(node, addOn);
		return aod;
	}

	void error(String key, Object... args) {
		error.visibleProperty().set(key != null);
		if (key == null) {
			error.visibleProperty().set(false);
			error.textProperty().set("");
		} else {
			error.visibleProperty().set(true);
			String txt = bundle.getString(key);
			if (args.length > 0) {
				txt = MessageFormat.format(txt, (Object[]) args);
			}
			error.textProperty().set(bundle.getString("errorIcon") + (txt == null ? "<missing key " + key + ">" : txt));
		}
	}

	void updateSelected() {
		AddOn addOn = getSelectedAddOn();
		if (addOn == null) {
			addOnDetailsContainer.visibleProperty().set(false);
		} else {
			systemAddOn.visibleProperty().set(addOn.isSystem());
			deleteAddOn.visibleProperty().set(!addOn.isSystem());
			addOnName.textProperty().set(addOn.getName());
			addOnDetailsContainer.visibleProperty().set(true);
			addOnType.textProperty().set(bundle.getString("addOnType." + addOn.getClass().getSimpleName()));
			addOnDetailsContainer.visibleProperty().set(true);
			by.visibleProperty().set(StringUtils.isNotBlank(addOn.getAuthor()));
			by.textProperty().set(MessageFormat.format(bundle.getString("by"), addOn.getAuthor()));
			description.visibleProperty().set(StringUtils.isNotBlank(addOn.getDescription()));
			description.textProperty().set(addOn.getDescription());
			license.visibleProperty().set(StringUtils.isNotBlank(addOn.getLicense()));
			license.textProperty().set(addOn.getLicense());
			url.visibleProperty().set(StringUtils.isNotBlank(addOn.getUrl()));
			url.textProperty().set(addOn.getUrl());
		}
	}

	private AddOn getSelectedAddOn() {
		Node addOnNode = installed.getSelectionModel().getSelectedItem();
		AddOn addOn = addOnNode == null ? null : addOnMap.get(addOnNode);
		return addOn;
	}

	@FXML
	void evtDeleteAddOn(ActionEvent evt) {
		AddOn addOn = getSelectedAddOn();
		Node addOnNode = installed.getSelectionModel().getSelectedItem();
		Confirm confirm = context.push(Confirm.class, Direction.FADE_IN);
		confirm.confirm(bundle, "deleteAddOn", () -> {
			try {
				context.getAddOnManager().uninstall(addOn);
				UIHelpers.zoomTo(installed, addOnNode);
				FadeTransition anim = new FadeTransition(Duration.seconds(1));
				anim.setCycleCount(1);
				anim.setNode(addOnNode);
				anim.setFromValue(1);
				anim.setToValue(0);
				anim.play();
				anim.onFinishedProperty().set((e) -> {
					installed.itemsProperty().get().remove(addOnNode);
					if (installed.itemsProperty().get().size() > 0)
						UIHelpers.zoomTo(installed, installed.itemsProperty().get().get(0));
				});
			} catch (IOException e) {
				LOG.log(java.lang.System.Logger.Level.ERROR, "Failed to delete add-on.", e);
				error("failedToDeleteAddOn", e.getMessage());
			}
		}, addOn.getName());
	}

	@FXML
	void evtAdd(ActionEvent evt) {
		error(null);
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("selectAddOn"));
		var path = PREFS.get("lastAddOnLocation", System.getProperty("user.dir") + File.separator + "add-on.jar");
		fileChooser.getExtensionFilters()
				.add(new ExtensionFilter(bundle.getString("addOnFileExtension"), "*.plugin.groovy", "*.jar", "*.zip"));
		fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("allFileExtensions"), "*.*"));
		UIHelpers.selectFilesDir(fileChooser, path);
		File file = fileChooser.showOpenDialog((Stage) getScene().getWindow());
		if (file != null) {
			PREFS.put("lastAddOnLocation", file.getAbsolutePath());
			try {
				AddOn addOn = context.getAddOnManager().install(file);
				Controller c = addAddOn(addOn);

				Parent root = c.getScene().getRoot();
				UIHelpers.zoomTo(installed, root);
				FadeTransition anim = new FadeTransition(Duration.seconds(3));
				anim.setCycleCount(1);
				anim.setNode(root);
				anim.setFromValue(0.1);
				anim.setToValue(1);
				anim.play();

				LOG.log(java.lang.System.Logger.Level.INFO, String.format("Installed %s add-on", file));
			} catch (Exception e) {
				LOG.log(java.lang.System.Logger.Level.ERROR, "Failed to add add-on.", e);
				error("failedToInstallAddOn", e.getMessage());
			}
		}
	}

	@FXML
	void evtUrl(ActionEvent evt) {
		error(null);
		AddOn addOn = getSelectedAddOn();
		context.getHostServices().showDocument(addOn.getUrl());
	}

	@FXML
	void evtBack(ActionEvent evt) {
		context.pop();
	}
}
