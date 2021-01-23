package uk.co.bithatch.snake.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Orientation;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import uk.co.bithatch.snake.lib.BrandingImage;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.layouts.Accessory;
import uk.co.bithatch.snake.lib.layouts.Accessory.AccessoryType;
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.DeviceLayout;
import uk.co.bithatch.snake.lib.layouts.DeviceLayouts.Listener;
import uk.co.bithatch.snake.lib.layouts.DeviceView;
import uk.co.bithatch.snake.lib.layouts.IO;
import uk.co.bithatch.snake.lib.layouts.ViewPosition;
import uk.co.bithatch.snake.widgets.Direction;
import uk.co.bithatch.snake.widgets.JavaFX;

public class DeviceDetails extends AbstractDetailsController implements Listener, PreferenceChangeListener {
	final static ResourceBundle bundle = ResourceBundle.getBundle(DeviceDetails.class.getName());

	private static final String PREF_USE_LAYOUT_VIEW = "useLayoutView";

	@FXML
	private FlowPane controls;
	@FXML
	private BorderPane header;
	@FXML
	private Hyperlink macros;
	@FXML
	private Hyperlink design;
	@FXML
	private Region background;
	@FXML
	private HBox decoratedTools;
	@FXML
	private HBox layoutTools;
	@FXML
	private BorderPane centre;
	@FXML
	private Hyperlink layoutView;
	@FXML
	private Hyperlink standardView;

	private List<Controller> deviceControls = new ArrayList<>();
	private boolean useLayoutView;

	@Override
	protected void onConfigure() {
	}

	public FlowPane getControls() {
		return controls;
	}

	@Override
	protected void onSetDeviceDetails() throws Exception {

		Device device = getDevice();
		JavaFX.bindManagedToVisible(macros, design, decoratedTools, standardView, layoutView, layoutTools);

		decoratedTools.visibleProperty().set(context.getConfiguration().isDecorated());
		context.getConfiguration().getNode().addPreferenceChangeListener(this);
		layoutTools.setVisible(hasLayout());
		checkLayoutStatus();

		/*
		 * Show macros link if device has the capability and there is not a layout that
		 * has a ACCESSORY of type PROFILES
		 */
		if (device.getCapabilities().contains(Capability.MACROS) || context.getMacroManager().isSupported(device)) {
			if (context.getLayouts().hasLayout(device)) {
				macros.setVisible(!hasLayoutProfileAccessory());
			} else {
				macros.setVisible(true);
			}
		} else {
			macros.setVisible(false);
		}

		boolean hasLayout = hasLayout();
		useLayoutView = hasLayout
				&& context.getPreferences(getDevice().getName()).getBoolean(PREF_USE_LAYOUT_VIEW, hasLayout);
		createView();

		context.getLayouts().addListener(this);

		if (!context.getMacroManager().isStarted() && !context.peek().equals(this)) {
			notifyMessage(MessagePersistence.ONCE_PER_RUNTIME, MessageType.WARNING, null,
					bundle.getString("warning.noUInput"), 60);
		}
		updateLayout();
	}

	protected boolean hasLayoutProfileAccessory() {
		DeviceView view = context.getLayouts().getLayout(getDevice()).getViewThatHas(ComponentType.ACCESSORY);
		if (view == null) {
			return false;
		} else {
			List<IO> els = view.getElements(ComponentType.ACCESSORY);
			boolean hasProfiles = false;
			for (IO el : els) {
				Accessory acc = (Accessory) el;
				if (acc.getAccessory() == AccessoryType.PROFILES) {
					hasProfiles = true;
					break;
				}
			}
			return hasProfiles;
		}
	}

	protected boolean hasLayout() {
		return context.getLayouts().hasLayout(getDevice())
				&& context.getLayouts().getLayout(getDevice()).getViewThatHas(ComponentType.AREA) != null;
	}

	protected void createView() {
		try {
			cleanUpControllers();
			deviceControls.clear();

			Device device = getDevice();

			boolean useLayout = useLayoutView;
			boolean hasEffects = device.getCapabilities().contains(Capability.EFFECTS)
					&& !device.getSupportedEffects().isEmpty();

			ScrollPane scroller = new ScrollPane();
			scroller.getStyleClass().add("transparentBackground");
			scroller.getStyleClass().add("focusless");
			scroller.setHbarPolicy(ScrollBarPolicy.NEVER);
			scroller.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

			Pane controlContainer;

			if (hasEffects && useLayout) {
				/* The layout takes up the bulk of the space */
				LayoutControl layoutControl = context.openScene(LayoutControl.class);
				layoutControl.setDevice(device);

				centre.setCenter(layoutControl.getScene().getRoot());
				deviceControls.add(layoutControl);

				/* Everything else goes in a scrolled area */
				controlContainer = new VBox();
				controlContainer.getStyleClass().add("controls-layout");
				controlContainer.getStyleClass().add("column");

				centre.setRight(scroller);
			} else {
				/* No layout, so all controls are in a horizontally flowing scroll pane */
				scroller.setHbarPolicy(ScrollBarPolicy.NEVER);
				scroller.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

				FlowPane flow = new FlowPane();
				flow.getStyleClass().add("spaced");
				flow.prefWrapLengthProperty().bind(scroller.widthProperty());
				flow.setOrientation(Orientation.HORIZONTAL);

				centre.setCenter(scroller);
				centre.setRight(null);
				controlContainer = flow;
				controlContainer.getStyleClass().add("controls-nolayout");
			}

			scroller.setContent(controlContainer);

			if (!useLayout) {
				if (hasEffects) {
					EffectsControl effectsControl = context.openScene(EffectsControl.class);
					effectsControl.setDevice(device);
					controlContainer.getChildren().add(effectsControl.getScene().getRoot());
					deviceControls.add(effectsControl);
				}

				if (device.getCapabilities().contains(Capability.BRIGHTNESS)) {
					BrightnessControl brightnessControl = context.openScene(BrightnessControl.class);
					brightnessControl.setDevice(device);
					controlContainer.getChildren().add(brightnessControl.getScene().getRoot());
					deviceControls.add(brightnessControl);
				}

				String imageUrl = context.getCache().getCachedImage(
						context.getDefaultImage(device.getType(), device.getImageUrl(BrandingImage.PERSPECTIVE)));
				if (imageUrl != null) {
					Background bg = new Background(new BackgroundImage(new Image(imageUrl, true),
							BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
							new BackgroundSize(100d, 100d, true, true, false, true)));
					background.setBackground(bg);
					background.opacityProperty().set(0.15);
					ColorAdjust adjust = new ColorAdjust();
					adjust.setSaturation(-1);
					background.setEffect(adjust);
				} else {
					background.setBackground(null);
					background.setEffect(null);
				}
			} else {
				background.setBackground(null);
				background.setEffect(null);
			}

			if (device.getCapabilities().contains(Capability.BATTERY)) {
				BatteryControl batteryControl = context.openScene(BatteryControl.class);
				batteryControl.setDevice(device);
				controlContainer.getChildren().add(batteryControl.getScene().getRoot());
				deviceControls.add(batteryControl);
			}

			if (device.getCapabilities().contains(Capability.DPI)) {
				DPIControl dpiControl = context.openScene(DPIControl.class);
				dpiControl.setDevice(device);
				controlContainer.getChildren().add(dpiControl.getScene().getRoot());
				deviceControls.add(dpiControl);
			}

			if (context.getAudioManager().hasVolume(device)) {
				VolumeControl volumeControl = context.openScene(VolumeControl.class);
				volumeControl.setDevice(device);
				controlContainer.getChildren().add(volumeControl.getScene().getRoot());
				deviceControls.add(volumeControl);
			}

			if (!useLayout || !hasLayoutProfileAccessory()) {
				if (context.getMacroManager().isSupported(device)) {
					MacroProfileControl profileControl = context.openScene(MacroProfileControl.class);
					profileControl.setDevice(device);
					controlContainer.getChildren().add(profileControl.getScene().getRoot());
					deviceControls.add(profileControl);
				} else if (device.getCapabilities().contains(Capability.MACRO_PROFILES)) {
					ProfileControl profileControl = context.openScene(ProfileControl.class);
					profileControl.setDevice(device);
					controlContainer.getChildren().add(profileControl.getScene().getRoot());
					deviceControls.add(profileControl);
				}
			}

			if (device.getCapabilities().contains(Capability.POLL_RATE)) {
				PollRateControl pollRateControl = context.openScene(PollRateControl.class);
				pollRateControl.setDevice(device);
				controlContainer.getChildren().add(pollRateControl.getScene().getRoot());
				deviceControls.add(pollRateControl);
			}

			if (device.getCapabilities().contains(Capability.GAME_MODE)) {
				GameModeControl gameModeControl = context.openScene(GameModeControl.class);
				gameModeControl.setDevice(device);
				controlContainer.getChildren().add(gameModeControl.getScene().getRoot());
				deviceControls.add(gameModeControl);
			}
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to create view.", e);
		}
	}

	protected void cleanUpControllers() {
		for (Controller c : deviceControls)
			c.cleanUp();
	}

	protected void checkLayoutStatus() {
		Device device = getDevice();
		boolean hasLayout = context.getLayouts().hasLayout(device);
		boolean hasUserLayout = context.getLayouts().hasUserLayout(device);
		boolean hasOfficial = context.getLayouts().hasOfficialLayout(device);
		if (!hasLayout) {
			notifyMessage(MessagePersistence.ONCE_PER_RUNTIME, MessageType.INFO,
					bundle.getString("info.missingLayout"));
		} else if (hasLayout && hasUserLayout && hasOfficial) {
			notifyMessage(MessagePersistence.ONCE_PER_RUNTIME, MessageType.INFO,
					bundle.getString("info.maskingOfficial"));
		} else {
			clearNotifications(MessageType.INFO, false);
		}
		layoutTools.setVisible(hasLayout());
		design.visibleProperty().set(!hasLayout || (hasLayout && hasUserLayout));
	}

	@Override
	protected void onDeviceCleanUp() {
		cleanUpControllers();
		deviceControls.clear();
		context.getLayouts().removeListener(this);
		context.getConfiguration().getNode().removePreferenceChangeListener(this);
	}

	@FXML
	void evtStandardView() {
		useLayoutView = false;
		updateLayout();
	}

	@FXML
	void evtLayoutView() {
		useLayoutView = true;
		updateLayout();
	}

	@FXML
	void evtAbout() {
		context.push(About.class, Direction.FADE);
	}

	@FXML
	void evtDesign() throws Exception {
		LayoutDesigner designer = context.push(LayoutDesigner.class, Direction.FROM_BOTTOM);
		DeviceLayoutManager layouts = context.getLayouts();
		Device device = getDevice();
		boolean hasLayout = layouts.hasLayout(device);
		DeviceLayout layout = null;
		if (!hasLayout) {
			layout = new DeviceLayout(device);
			DeviceView view = new DeviceView();
			view.setPosition(ViewPosition.TOP);
			layout.addView(view);
			layouts.addLayout(layout);
		} else {
			layout = layouts.getLayout(device);
		}
		designer.setLayout(layout);
	}

	@FXML
	void evtOptions() {
		context.push(Options.class, Direction.FROM_BOTTOM);
	}

	@FXML
	void evtMacros() {
		try {
			context.editMacros(this);
		} catch (Exception e) {
			error(e);
		}
	}

	public void configure(Lit lit, EffectHandler<?, ?> configurable) {
		throw new UnsupportedOperationException("TODO: Load effect configuration");
	}

	@Override
	public void layoutChanged(DeviceLayout layout) {
		if (Platform.isFxApplicationThread()) {
			checkLayoutStatus();
			return;
		}
		Platform.runLater(() -> layoutChanged(layout));
	}

	@Override
	public void layoutAdded(DeviceLayout layout) {
		layoutChanged(layout);
	}

	@Override
	public void layoutRemoved(DeviceLayout layout) {
		layoutChanged(layout);
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent evt) {
		if (evt.getKey().equals(Configuration.PREF_DECORATED)) {
			decoratedTools.visibleProperty().set(context.getConfiguration().isDecorated());
		}
	}

	void updateLayout() {
		standardView.setVisible(useLayoutView);
		layoutView.setVisible(!useLayoutView);
		createView();
	}
}
