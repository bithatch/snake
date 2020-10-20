package uk.co.bithatch.snake.ui;

import java.io.IOException;
import java.lang.System.Logger.Level;
import java.text.MessageFormat;
import java.util.ResourceBundle;

import javafx.animation.FadeTransition;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.ui.Configuration.TrayIcon;

public class Options extends AbstractDeviceController {

	final static System.Logger LOG = System.getLogger(Options.class.getName());
	final static ResourceBundle bundle = ResourceBundle.getBundle(Options.class.getName());

	@FXML
	private ColorPicker color;
	@FXML
	private CheckBox decorated;
	@FXML
	private ComboBox<Theme> theme;
	@FXML
	private BorderPane optionsHeader;
	@FXML
	private Label transparencyLabel;
	@FXML
	private Slider transparency;
	@FXML
	private RadioButton noTrayIcon;
	@FXML
	private RadioButton autoTrayIcon;
	@FXML
	private RadioButton darkTrayIcon;
	@FXML
	private RadioButton lightTrayIcon;
	@FXML
	private RadioButton colorTrayIcon;
	@FXML
	private Label noTrayIconLabel;
	@FXML
	private Label autoTrayIconLabel;
	@FXML
	private Label darkTrayIconLabel;
	@FXML
	private Label lightTrayIconLabel;
	@FXML
	private Label colorTrayIconLabel;
	@FXML
	private CheckBox showBattery;
	@FXML
	private CheckBox whenLow;
	@FXML
	private CheckBox startOnLogin;
	@FXML
	private ToggleGroup trayIconGroup;
	@FXML
	private CheckBox betas;
	@FXML
	private CheckBox updateAutomatically;
	@FXML
	private CheckBox checkForUpdates;
	@FXML
	private VBox updates;
	@FXML
	private Hyperlink startUpdate;
	@FXML
	private HBox updatesContainer;
	@FXML
	private Label availableVersion;
	@FXML
	private Label installedVersion;

	@Override
	protected void onConfigure() throws Exception {
		noTrayIconLabel.setLabelFor(noTrayIcon);
		autoTrayIconLabel.setLabelFor(autoTrayIcon);
		darkTrayIconLabel.setLabelFor(darkTrayIcon);
		lightTrayIconLabel.setLabelFor(lightTrayIcon);
		colorTrayIconLabel.setLabelFor(colorTrayIcon);
		Configuration cfg = Configuration.getDefault();
		startOnLogin.selectedProperty().addListener((e) -> setStartOnLogin(cfg));
		startOnLogin.visibleProperty().set(PlatformService.isPlatformSupported());
		whenLow.visibleProperty().bind(showBattery.visibleProperty());
		whenLow.managedProperty().bind(whenLow.visibleProperty());
		showBattery.managedProperty().bind(showBattery.visibleProperty());
		theme.itemsProperty().get().addAll(Theme.getThemes());
		showBattery.visibleProperty().set(context.getBackend().getCapabilities().contains(Capability.BATTERY));
		if (PlatformService.isPlatformSupported()) {
			PlatformService ps = PlatformService.get();
			startOnLogin.selectedProperty().set(ps.isStartOnLogin());
			updatesContainer.visibleProperty().set(ps.isUpdateableApp());
			updateAutomatically.selectedProperty().set(ps.isUpdateAutomatically());
			updateAutomatically.selectedProperty()
					.addListener((e) -> ps.setUpdateAutomatically(updateAutomatically.selectedProperty().get()));
			checkForUpdates.selectedProperty().set(ps.isCheckForUpdates());
			checkForUpdates.selectedProperty().addListener((e) -> {
				ps.setCheckForUpdates(checkForUpdates.selectedProperty().get());
				updates.visibleProperty().set(ps.isUpdateAvailable() && ps.isCheckForUpdates());
			});
			betas.selectedProperty().set(ps.isBetas());
			betas.selectedProperty().addListener((e) -> {
				ps.setBetas(betas.selectedProperty().get());
//				updateAutomatically.disableProperty().set(true);
//				checkForUpdates.disableProperty().set(true);
//				betas.disableProperty().set(true);
				startUpdate.disableProperty().set(true);
				new Thread() {
					public void run() {
						try {
							Thread.sleep(3000);
						} catch (Exception e) {
						}
						System.exit(90);
					}
				}.start();
			});
			updates.visibleProperty().set(ps.isUpdateAvailable() && ps.isCheckForUpdates());
			updates.managedProperty().bind(updates.visibleProperty());
			updates.getStyleClass().add("warning");

			if (ps.isUpdateAvailable()) {
				FadeTransition anim = new FadeTransition(Duration.seconds(5));
				anim.setAutoReverse(true);
				anim.setCycleCount(FadeTransition.INDEFINITE);
				anim.setNode(updates);
				anim.setFromValue(0.5);
				anim.setToValue(1);
				anim.play();
				availableVersion.textProperty().set(MessageFormat.format(bundle.getString("availableVersion"),
						PlatformService.get().getAvailableVersion()));
				installedVersion.textProperty().set(MessageFormat.format(bundle.getString("installedVersion"),
						PlatformService.get().getInstalledVersion()));
			} else {
				availableVersion.textProperty().set("");
				installedVersion.textProperty().set("");
			}

		} else {
			updatesContainer.visibleProperty().set(false);
		}
		betas.disableProperty().bind(Bindings.not(checkForUpdates.selectedProperty()));
		updateAutomatically.disableProperty().bind(Bindings.not(checkForUpdates.selectedProperty()));
		checkForUpdates.managedProperty().bind(checkForUpdates.visibleProperty());
		startOnLogin.selectedProperty().addListener((e) -> {
			try {
				PlatformService.get().setStartOnLogin(startOnLogin.selectedProperty().get());
			} catch (IOException e1) {
				LOG.log(Level.ERROR, "Failed to set start on login state.", e);
			}
		});
		optionsHeader.setBackground(createHeaderBackground());
		decorated.selectedProperty().bindBidirectional(cfg.decoratedProperty());
		theme.getSelectionModel().select(cfg.themeProperty().getValue());
		transparencyLabel.setLabelFor(transparency);
		transparency.disableProperty().bind(decorated.selectedProperty());
		transparency.valueProperty().bindBidirectional(cfg.transparencyProperty());
		if (context.getWindow() != null) {
			context.getWindow().getOptions().visibleProperty().set(false);
		}
		trayIconGroup.selectedToggleProperty().addListener((e) -> {
			if (noTrayIcon.selectedProperty().get())
				cfg.trayIconProperty().setValue(TrayIcon.OFF);
			else if (autoTrayIcon.selectedProperty().get())
				cfg.trayIconProperty().setValue(TrayIcon.AUTO);
			else if (darkTrayIcon.selectedProperty().get())
				cfg.trayIconProperty().setValue(TrayIcon.DARK);
			else if (lightTrayIcon.selectedProperty().get())
				cfg.trayIconProperty().setValue(TrayIcon.LIGHT);
			else if (colorTrayIcon.selectedProperty().get())
				cfg.trayIconProperty().setValue(TrayIcon.COLOR);
			setAvailable(cfg);
			setStartOnLogin(cfg);
		});
		showBattery.selectedProperty().bindBidirectional(cfg.showBatteryProperty());
		whenLow.selectedProperty().bindBidirectional(cfg.whenLowProperty());
		whenLow.disableProperty().bind(Bindings.not(showBattery.selectedProperty()));
		switch (cfg.trayIconProperty().getValue()) {
		case OFF:
			noTrayIcon.selectedProperty().set(true);
			break;
		case AUTO:
			autoTrayIcon.selectedProperty().set(true);
			break;
		case DARK:
			darkTrayIcon.selectedProperty().set(true);
			break;
		case LIGHT:
			lightTrayIcon.selectedProperty().set(true);
			break;
		default:
			colorTrayIcon.selectedProperty().set(true);
			break;
		}
		setAvailable(cfg);
	}

	private void setStartOnLogin(Configuration cfg) {
		try {
			PlatformService.get().setStartOnLogin(
					cfg.trayIconProperty().getValue() != TrayIcon.OFF && startOnLogin.selectedProperty().get());
		} catch (IOException e) {
			LOG.log(Level.ERROR, "Failed to set start on login state.", e);
		}
	}

	private void setAvailable(Configuration cfg) {
		showBattery.disableProperty().set(cfg.trayIconProperty().getValue() == TrayIcon.OFF);
		startOnLogin.disableProperty().set(cfg.trayIconProperty().getValue() == TrayIcon.OFF);
	}

	@FXML
	void evtStartUpdate(ActionEvent evt) {
		System.exit(100);
	}

	@FXML
	void evtTheme(ActionEvent evt) {
		Configuration.getDefault().themeProperty().setValue(theme.getSelectionModel().getSelectedItem());
	}

	@FXML
	void evtBack(ActionEvent evt) {
		if (context.getWindow() != null) {
			context.getWindow().getOptions().visibleProperty().set(true);
		}
		context.pop();
	}
}
