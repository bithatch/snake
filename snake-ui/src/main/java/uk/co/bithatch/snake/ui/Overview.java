package uk.co.bithatch.snake.ui;

import java.lang.System.Logger.Level;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import org.controlsfx.control.ToggleSwitch;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import uk.co.bithatch.snake.lib.Backend.BackendListener;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Device.Listener;
import uk.co.bithatch.snake.lib.DeviceType;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.binding.Profile;
import uk.co.bithatch.snake.lib.binding.ProfileMap;
import uk.co.bithatch.snake.widgets.Direction;
import uk.co.bithatch.snake.widgets.ImageButton;
import uk.co.bithatch.snake.widgets.JavaFX;

public class Overview extends AbstractController implements Listener, BackendListener, PreferenceChangeListener {

	final static ResourceBundle bundle = ResourceBundle.getBundle(Overview.class.getName());

	private static final int MIN_ITEMS_FOR_SEARCH = 3;

	@FXML
	private Label battery;
	@FXML
	private Slider brightness;
	@FXML
	private Hyperlink clearFilter;
	@FXML
	private BorderPane content;
	@FXML
	private HBox decoratedTools;
	@FXML
	private ListView<Node> devices;
	@FXML
	private TextField filter;
	@FXML
	private BorderPane filterOptions;
	@FXML
	private BorderPane header;
	@FXML
	private BorderPane overviewContent;
	@FXML
	private ToggleSwitch sync;
	@FXML
	private HBox types;

	@FXML
	private Hyperlink update;

	// private BorderPane tempFilterOptions;
	private boolean adjustingBrightness;
	private List<Device> deviceList = new ArrayList<>();
	private Map<Node, DeviceOverview> deviceMap = new HashMap<>();
	private Map<DeviceType, List<Device>> deviceTypeMap = new HashMap<>();
	private List<DeviceType> filteredTypes = new ArrayList<>();
	private Timeline filterTimer;
	private Timeline brightnessTimer;
	private FadeTransition batteryFader;

	@Override
	public void activeMapChanged(ProfileMap map) {
	}

	@Override
	public void changed(Device device, Region region) {
		Platform.runLater(() -> {
			rebuildBattery();
			if (!adjustingBrightness) {
				adjustingBrightness = true;
				try {
					brightness.valueProperty().set(context.getBackend().getBrightness());
				} catch(Exception e) {
					//
				} finally {
					adjustingBrightness = false;
				}
			}
		});
	}

	@Override
	public void deviceAdded(Device device) {
		Platform.runLater(() -> {
			devicesChanged();
		});
	}

	@Override
	public void deviceRemoved(Device device) {
		Platform.runLater(() -> {
			devicesChanged();
		});
	}

	@Override
	public void mapAdded(ProfileMap profile) {
	}

	@Override
	public void mapChanged(ProfileMap profile) {
	}

	@Override
	public void mapRemoved(ProfileMap profile) {
	}

	@Override
	public void profileAdded(Profile profile) {
	}

	@Override
	public void profileRemoved(Profile profile) {
	}

	protected void devicesChanged() {
		try {
			rebuildDevices();
			rebuildFilterTypes();
			rebuildBattery();
			filter();
		} catch (Exception e) {
			LOG.log(Level.ERROR, "Failed to update devices.", e);
		}
	}

	@Override
	protected void onCleanUp() {
		for (DeviceOverview dov : deviceMap.values())
			dov.cleanUp();
		deviceMap.clear();
		context.getBackend().removeListener(this);
		context.getConfiguration().getNode().removePreferenceChangeListener(this);
	}

	@Override
	protected void onConfigure() throws Exception {
		super.onConfigure();

		JavaFX.bindManagedToVisible(filterOptions);
		decoratedTools.visibleProperty().set(context.getConfiguration().isDecorated());
		context.getConfiguration().getNode().addPreferenceChangeListener(this);

		filterOptions.setBackground(createHeaderBackground());

		batteryFader = BatteryControl.createFader(battery);

		rebuildDevices();
		rebuildFilterTypes();
		rebuildBattery();
		filter();

		context.getBackend().addListener(this);

		brightness.valueProperty().set(context.getBackend().getBrightness());
		brightness.valueProperty().addListener((e) -> {
			if (!adjustingBrightness) {
				/*
				 * If there are more than a handfule of devices, instead put the update on a
				 * timer, as it can get pretty slow (e.g. with all fake drivers enabled, the 70
				 * ish devices make the slider a bit jerky)
				 */
				if (deviceList != null && deviceList.size() > 10) {
					resetBrightnessTimer((ae) -> updateBrightness());
				} else {
					updateBrightness();
				}
			}
		});

		devices.setOnMouseClicked((e) -> {
			if (e.getClickCount() == 2) {
				context.push(DeviceDetails.class, this, Direction.FROM_RIGHT)
						.setDevice(deviceMap.get(devices.getSelectionModel().getSelectedItem()).getDevice());
			}

		});
		sync.setSelected(context.getEffectManager().isSync());
		filter.textProperty().addListener((e) -> resetFilterTimer());

		clearFilter.onActionProperty().set((e) -> {
			filter.textProperty().set("");
			cancelFilterTimer();
			filter();
		});

		if (PlatformService.isPlatformSupported()) {
			PlatformService ps = PlatformService.get();
			if (ps.isUpdateAvailable()) {
				update.visibleProperty().set(true);
				FadeTransition anim = new FadeTransition(Duration.seconds(5));
				anim.setAutoReverse(true);
				anim.setCycleCount(FadeTransition.INDEFINITE);
				anim.setNode(update);
				anim.setFromValue(0.5);
				anim.setToValue(1);
				anim.play();
			} else
				update.visibleProperty().set(false);
		} else
			update.visibleProperty().set(false);

		if (!context.getMacroManager().isStarted()) {
			notifyMessage(MessagePersistence.ONCE_PER_RUNTIME, MessageType.WARNING, null,
					bundle.getString("warning.noUInput"), 60);
		}
	}

	void cancelBrightnessTimer() {
		if (brightnessTimer != null) {
			brightnessTimer.stop();
			brightnessTimer = null;
		}
	}

	void cancelFilterTimer() {
		if (filterTimer != null) {
			filterTimer.stop();
			filterTimer = null;
		}
	}

	@FXML
	void evtAbout() {
		context.push(About.class, Direction.FADE);
	}

	@FXML
	void evtOptions() {
		context.push(Options.class, Direction.FROM_BOTTOM);
	}
	
	@FXML
	void evtSync() {
		sync.selectedProperty().addListener((e) -> context.getEffectManager().setSync(sync.selectedProperty().get()));
	}
	
	@FXML
	void evtToggleSync() {
		sync.setSelected(!sync.isSelected());
	}

	@FXML
	void evtUpdate() {
		context.push(Options.class, Direction.FROM_BOTTOM);
	}

	void filter() {
		try {
			for (Map.Entry<Node, DeviceOverview> en : deviceMap.entrySet()) {
				en.getValue().cleanUp();
			}
			deviceMap.clear();
			devices.getItems().clear();
			String filterText = filter.textProperty().get().toLowerCase();
			for (Device device : deviceList) {
				if (matchesFilter(filterText, device) && matchesFilteredType(device)) {
					DeviceOverview overview = context.openScene(DeviceOverview.class);
					overview.setDevice(device);
					Parent node = overview.getScene().getRoot();
					devices.getItems().add(node);
					deviceMap.put(node, overview);
				}
			}
		} catch (Exception e) {
			throw new IllegalStateException("Failed to get devices.", e);
		}
	}

	void resetBrightnessTimer(EventHandler<ActionEvent> cb) {
		if (brightnessTimer == null) {
			brightnessTimer = new Timeline(new KeyFrame(Duration.millis(750), cb));
		}
		brightnessTimer.playFromStart();
	}

	void resetFilterTimer() {
		if (filterTimer == null) {
			filterTimer = new Timeline(new KeyFrame(Duration.millis(750), ae -> filter()));
		}
		filterTimer.playFromStart();
	}

	private boolean matchesFilter(String filterText, Device device) {
		return filterText.equals("") || device.getName().toLowerCase().contains(filterText)
				|| device.getDriverVersion().toLowerCase().contains(filterText)
				|| device.getSerial().toLowerCase().contains(filterText);
	}

	private boolean matchesFilteredType(Device device) {
		return deviceTypeMap.size() < 2 || filteredTypes.contains(device.getType());
	}

	private void rebuildBattery() {
		int batt = context.getBackend().getBattery();
		if (batt < 0) {
			battery.visibleProperty().set(false);
			batteryFader.stop();
		} else {
			battery.graphicProperty().set(new FontIcon(BatteryControl.getBatteryIcon(batt)));
			BatteryControl.setBatteryStatusStyle(context.getBackend().getLowBatteryThreshold(), batt, battery,
					batteryFader);
			battery.visibleProperty().set(true);
		}
	}

	private void rebuildDevices() throws Exception {
		for (Device dev : deviceList) {
			dev.removeListener(this);
		}
		deviceList = context.getBackend().getDevices();
		deviceTypeMap.clear();
		for (Device d : deviceList) {
			List<Device> dl = deviceTypeMap.get(d.getType());
			if (dl == null) {
				dl = new ArrayList<>();
				deviceTypeMap.put(d.getType(), dl);
			}
			dl.add(d);
			d.addListener(this);
		}
		filter.setVisible(deviceList.size() > MIN_ITEMS_FOR_SEARCH);
		clearFilter.setVisible(deviceList.size() > MIN_ITEMS_FOR_SEARCH);
		updateFilterOptions();
	}

	private void rebuildFilterTypes() {
		types.getChildren().clear();
		filteredTypes.clear();
		for (DeviceType type : DeviceType.values()) {
			if (type != DeviceType.UNRECOGNISED && deviceTypeMap.containsKey(type)) {
				URL typeImage = context.getConfiguration().getTheme().getDeviceImage(32, type);
				if (typeImage == null) {
					typeImage = context.getConfiguration().getTheme().getDeviceImage(32, DeviceType.UNRECOGNISED);
				}
				Image img = new Image(typeImage.toExternalForm(), 32, 32, true, true, true);
				ImageButton button = new ImageButton(img, 32, 32);
				button.onActionProperty().set((e) -> {
					if (filteredTypes.size() > 1 && filteredTypes.contains(type)) {
						button.opacityProperty().set(0.5);
						filteredTypes.remove(type);
					} else if (!filteredTypes.contains(type)) {
						button.opacityProperty().set(1);
						filteredTypes.add(type);
					}
					filter();
				});
				filteredTypes.add(type);
				types.getChildren().add(button);
			}
		}
		types.setVisible(types.getChildren().size() > 1);

		updateFilterOptions();

	}

	private void updateBrightness() {
		adjustingBrightness = true;
		try {
			context.getBackend().setBrightness((short) brightness.valueProperty().get());
		} finally {
			adjustingBrightness = false;
		}
	}

	private void updateFilterOptions() {
		boolean showFilter = deviceList.size() > MIN_ITEMS_FOR_SEARCH || filteredTypes.size() > 1;
		filterOptions.setVisible(showFilter);
//		if (showFilter && tempFilterOptions != null) {
//			content.getChildren().add(tempFilterOptions);
//			tempFilterOptions = null;
//		} else if (!showFilter && tempFilterOptions == null) {
//			content.getChildren().remove(filterOptions);
//			tempFilterOptions = filterOptions;
//		}
		content.layout();
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent evt) {
		if (evt.getKey().equals(Configuration.PREF_DECORATED))
			decoratedTools.setVisible(context.getConfiguration().isDecorated());

	}
}
