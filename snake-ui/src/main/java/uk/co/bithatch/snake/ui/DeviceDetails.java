package uk.co.bithatch.snake.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
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
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.DeviceLayout;
import uk.co.bithatch.snake.lib.layouts.DeviceLayouts.Listener;
import uk.co.bithatch.snake.ui.util.JavaFX;
import uk.co.bithatch.snake.ui.widgets.Direction;

public class DeviceDetails extends AbstractDetailsController implements Listener {
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

	private List<Controller> controllers = new ArrayList<>();
	private final BooleanProperty useLayoutView = new SimpleBooleanProperty();

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

		Property<Boolean> decProp = context.getConfiguration().decoratedProperty();
		decoratedTools.visibleProperty().set(decProp.getValue());
		context.getConfiguration().decoratedProperty()
				.addListener((e) -> decoratedTools.visibleProperty().set(decProp.getValue()));
		standardView.visibleProperty().bind(useLayoutView);
		layoutView.visibleProperty().bind(Bindings.not(useLayoutView));
		layoutTools.setVisible(hasLayout());
		checkLayoutStatus();

		macros.visibleProperty().set(device.getCapabilities().contains(Capability.MACROS));

		boolean hasLayout = hasLayout();
		useLayoutView.set(
				hasLayout && context.getPreferences(getDevice().getName()).getBoolean(PREF_USE_LAYOUT_VIEW, hasLayout));
		useLayoutView.addListener((c, o, n) -> {
			createView();
		});
		createView();

		context.getLayouts().addListener(this);
	}

	protected boolean hasLayout() {
		return context.getLayouts().hasLayout(getDevice())
				&& context.getLayouts().getLayout(getDevice()).getViewThatHas(ComponentType.AREA) != null;
	}

	protected void createView() {
		try {
			cleanUpControllers();
			controllers.clear();

			Device device = getDevice();

			boolean useLayout = useLayoutView.get();
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
				controllers.add(layoutControl);

				/* Everything else goes in a scrolled area */
				controlContainer = new VBox();
				centre.setRight(scroller);
				controlContainer.getStyleClass().add("controls-layout");
			} else {
				/* No layout, so all controls are in a horizontally flowing scroll pane */
				scroller.setHbarPolicy(ScrollBarPolicy.NEVER);
				scroller.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

				FlowPane flow = new FlowPane();
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
					controllers.add(effectsControl);
				}

				if (device.getCapabilities().contains(Capability.BRIGHTNESS)) {
					BrightnessControl brightnessControl = context.openScene(BrightnessControl.class);
					brightnessControl.setDevice(device);
					controlContainer.getChildren().add(brightnessControl.getScene().getRoot());
					controllers.add(brightnessControl);
				}

				String imageUrl = context.getCache().getCachedImage(context.getDefaultImage(device.getType(), device.getImageUrl(BrandingImage.PERSPECTIVE)));
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
				controllers.add(batteryControl);
			}

			if (device.getCapabilities().contains(Capability.DPI)) {
				DPIControl dpiControl = context.openScene(DPIControl.class);
				dpiControl.setDevice(device);
				controlContainer.getChildren().add(dpiControl.getScene().getRoot());
				controllers.add(dpiControl);
			}

			if (device.getCapabilities().contains(Capability.POLL_RATE)) {
				PollRateControl pollRateControl = context.openScene(PollRateControl.class);
				pollRateControl.setDevice(device);
				controlContainer.getChildren().add(pollRateControl.getScene().getRoot());
				controllers.add(pollRateControl);
			}

			if (device.getCapabilities().contains(Capability.GAME_MODE)) {
				GameModeControl gameModeControl = context.openScene(GameModeControl.class);
				gameModeControl.setDevice(device);
				controlContainer.getChildren().add(gameModeControl.getScene().getRoot());
				controllers.add(gameModeControl);
			}
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to create view.", e);
		}
	}

	protected void cleanUpControllers() {
		for (Controller c : controllers)
			c.cleanUp();
	}

	protected void checkLayoutStatus() {
		Device device = getDevice();
		boolean hasLayout = context.getLayouts().hasLayout(device);
		if (design.visibleProperty().get()) {
			if (hasLayout) {
				clearNotifications(MessageType.INFO, false);
			} else {
				notifyMessage(MessagePersistence.ONCE_PER_RUNTIME, MessageType.INFO,
						bundle.getString("info.missingLayout"));
			}
		}
		layoutTools.setVisible(hasLayout());
		design.visibleProperty().set(!hasLayout || context.getLayouts().hasUserLayout(device));
	}

	@Override
	protected void onDeviceCleanUp() {
		cleanUpControllers();
		controllers.clear();
		context.getLayouts().removeListener(this);
	}

	@FXML
	void evtStandardView() {
		useLayoutView.set(false);
	}

	@FXML
	void evtLayoutView() {
		useLayoutView.set(true);
	}

	@FXML
	void evtAbout() {
		context.push(About.class, Direction.FADE);
	}

	@FXML
	void evtDesign() {
		context.push(LayoutDesigner.class, Direction.FROM_BOTTOM);
	}

	@FXML
	void evtOptions() {
		context.push(Options.class, Direction.FROM_BOTTOM);
	}

	@FXML
	void evtMacros() {
		context.push(Macros.class, Direction.FROM_BOTTOM);
	}

	public void configure(Lit lit, EffectHandler<?, ?> configurable) {
		throw new UnsupportedOperationException("TODO: Load effect configuration");
	}

	@Override
	public void layoutChanged(DeviceLayout layout) {
		if (Platform.isFxApplicationThread()) {
			checkLayoutStatus();
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
}
