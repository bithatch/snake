package uk.co.bithatch.snake.ui;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.Property;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Device.Listener;
import uk.co.bithatch.snake.lib.DeviceType;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.ui.util.JavaFX;
import uk.co.bithatch.snake.ui.widgets.Direction;
import uk.co.bithatch.snake.ui.widgets.ImageButton;

public class Overview extends AbstractController implements Listener {

	private static final int MIN_ITEMS_FOR_SEARCH = 3;
	@FXML
	private ListView<Node> devices;
	@FXML
	private BorderPane overviewContent;
	@FXML
	private BorderPane header;
	@FXML
	private CheckBox sync;
	@FXML
	private HBox types;
	@FXML
	private Hyperlink clearFilter;
	@FXML
	private Hyperlink update;
	@FXML
	private TextField filter;
	@FXML
	private BorderPane filterOptions;
	@FXML
	private BorderPane content;
	@FXML
	private Slider brightness;
	@FXML
	private Label battery;
	@FXML
	private HBox decoratedTools;

	private FadeTransition batteryFader;
	private Map<Node, DeviceOverview> deviceMap = new HashMap<>();
	private Timeline brightnessTimer;
	private Timeline filterTimer;
	private List<DeviceType> filteredTypes = new ArrayList<>();
	private List<Device> deviceList = new ArrayList<>();
	private Map<DeviceType, List<Device>> deviceTypeMap = new HashMap<>();
//	private BorderPane tempFilterOptions;
	private boolean adjustingBrightness;

	final static ResourceBundle bundle = ResourceBundle.getBundle(Overview.class.getName());

	@Override
	protected void onConfigure() throws Exception {
		super.onConfigure();

		JavaFX.bindManagedToVisible(filterOptions);
		Property<Boolean> decProp = context.getConfiguration().decoratedProperty();
		decoratedTools.visibleProperty().set(decProp.getValue());
		context.getConfiguration().decoratedProperty()
				.addListener((e) -> decoratedTools.visibleProperty().set(decProp.getValue()));

		filterOptions.setBackground(createHeaderBackground());

		batteryFader = BatteryControl.createFader(battery);

		rebuildDevices();
		rebuildFilterTypes();
		rebuildBattery();
		filter();

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
		sync.selectedProperty().set(context.getBackend().isSync());
		sync.selectedProperty().addListener((e) -> context.getBackend().setSync(sync.selectedProperty().get()));
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
	}

	private void updateBrightness() {
		adjustingBrightness = true;
		try {
			context.getBackend().setBrightness((short) brightness.valueProperty().get());
		} finally {
			adjustingBrightness = false;
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

	private void rebuildBattery() {
		int batt = context.getBackend().getBattery();
		if (batt < 0) {
			battery.visibleProperty().set(false);
			batteryFader.stop();
		} else {
			battery.textProperty().set(BatteryControl.getBatteryIcon(batt));
			BatteryControl.setBatteryStatusStyle(context.getBackend().getLowBatteryThreshold(), batt, battery,
					batteryFader);
			battery.visibleProperty().set(true);
		}
	}

	private void rebuildFilterTypes() {
		types.getChildren().clear();
		filteredTypes.clear();
		for (DeviceType type : DeviceType.values()) {
			if (type != DeviceType.UNRECOGNISED && deviceTypeMap.containsKey(type)) {
				URL typeImage = context.getConfiguration().themeProperty().getValue().getDeviceImage(32, type);
				if (typeImage == null) {
					typeImage = context.getConfiguration().themeProperty().getValue().getDeviceImage(32,
							DeviceType.UNRECOGNISED);
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

	void cancelFilterTimer() {
		if (filterTimer != null) {
			filterTimer.stop();
			filterTimer = null;
		}
	}

	void cancelBrightnessTimer() {
		if (brightnessTimer != null) {
			brightnessTimer.stop();
			brightnessTimer = null;
		}
	}

	void resetFilterTimer() {
		if (filterTimer == null) {
			filterTimer = new Timeline(new KeyFrame(Duration.millis(750), ae -> filter()));
		}
		filterTimer.playFromStart();
	}

	void resetBrightnessTimer(EventHandler<ActionEvent> cb) {
		if (brightnessTimer == null) {
			brightnessTimer = new Timeline(new KeyFrame(Duration.millis(750), cb));
		}
		brightnessTimer.playFromStart();
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

	private boolean matchesFilteredType(Device device) {
		return deviceTypeMap.size() < 2 || filteredTypes.contains(device.getType());
	}

	private boolean matchesFilter(String filterText, Device device) {
		return filterText.equals("") || device.getName().toLowerCase().contains(filterText)
				|| device.getDriverVersion().toLowerCase().contains(filterText)
				|| device.getSerial().toLowerCase().contains(filterText);
	}

	@Override
	protected void onCleanUp() {
		for (DeviceOverview dov : deviceMap.values())
			dov.cleanUp();
		deviceMap.clear();
	}

	@Override
	public void changed(Device device, Region region) {
		rebuildBattery();
		if (!adjustingBrightness) {
			adjustingBrightness = true;
			try {
				brightness.valueProperty().set(context.getBackend().getBrightness());
			} finally {
				adjustingBrightness = false;
			}
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
	void evtUpdate() {
		context.push(Options.class, Direction.FROM_BOTTOM);
	}
}
