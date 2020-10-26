package uk.co.bithatch.snake.ui;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ScrollPane;
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
import javafx.scene.layout.Region;
import uk.co.bithatch.snake.lib.BrandingImage;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.ui.SlideyStack.Direction;

public class DeviceDetails extends AbstractDetailsController {

	@FXML
	private FlowPane controls;
	@FXML
	private ScrollPane scroll;
	@FXML
	private BorderPane header;
	@FXML
	private Hyperlink macros;
	@FXML
	private Region background;
	@FXML
	private HBox decoratedTools;

	private List<Controller> controllers = new ArrayList<>();

	@Override
	protected void onConfigure() {
		controls.prefWidthProperty().bind(Bindings.add(-5, scroll.widthProperty()));
		controls.prefHeightProperty().bind(Bindings.add(-5, scroll.heightProperty()));
	}

	public FlowPane getControls() {
		return controls;
	}

	@Override
	protected void onSetDeviceDetails() throws Exception {
		Property<Boolean> decProp = context.getConfiguration().decoratedProperty();
		macros.managedProperty().bind(macros.visibleProperty());
		decoratedTools.managedProperty().bind(decoratedTools.visibleProperty());
		decoratedTools.visibleProperty().set(decProp.getValue());
		context.getConfiguration().decoratedProperty()
				.addListener((e) -> decoratedTools.visibleProperty().set(decProp.getValue()));

		String imageUrl = getDevice().getImageUrl(BrandingImage.PERSPECTIVE);
		if(imageUrl != null) {
			Background bg = new Background(
					new BackgroundImage(new Image(imageUrl, true), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT,
							BackgroundPosition.CENTER, new BackgroundSize(100d, 100d, true, true, false, true)));
			background.setBackground(bg);
			background.opacityProperty().set(0.15);
			ColorAdjust adjust = new ColorAdjust();
			adjust.setSaturation(-1);
			background.setEffect(adjust);
		}

		macros.visibleProperty().set(getDevice().getCapabilities().contains(Capability.MACROS));

		controls.getChildren().clear();

		if (getDevice().getCapabilities().contains(Capability.EFFECTS)
				&& !getDevice().getSupportedEffects().isEmpty()) {
			EffectsControl effectsControl = context.openScene(EffectsControl.class);
			effectsControl.setDevice(getDevice());
			controls.getChildren().add(effectsControl.getScene().getRoot());
			controllers.add(effectsControl);
		}

		if (getDevice().getCapabilities().contains(Capability.BRIGHTNESS)) {
			BrightnessControl brightnessControl = context.openScene(BrightnessControl.class);
			brightnessControl.setDevice(getDevice());
			controls.getChildren().add(brightnessControl.getScene().getRoot());
			controllers.add(brightnessControl);
		}

		if (getDevice().getCapabilities().contains(Capability.BATTERY)) {
			BatteryControl batteryControl = context.openScene(BatteryControl.class);
			batteryControl.setDevice(getDevice());
			controls.getChildren().add(batteryControl.getScene().getRoot());
			controllers.add(batteryControl);
		}

		if (getDevice().getCapabilities().contains(Capability.DPI)) {
			DPIControl dpiControl = context.openScene(DPIControl.class);
			dpiControl.setDevice(getDevice());
			controls.getChildren().add(dpiControl.getScene().getRoot());
			controllers.add(dpiControl);
		}

		if (getDevice().getCapabilities().contains(Capability.POLL_RATE)) {
			PollRateControl pollRateControl = context.openScene(PollRateControl.class);
			pollRateControl.setDevice(getDevice());
			controls.getChildren().add(pollRateControl.getScene().getRoot());
			controllers.add(pollRateControl);
		}

		if (getDevice().getCapabilities().contains(Capability.GAME_MODE)) {
			GameModeControl gameModeControl = context.openScene(GameModeControl.class);
			gameModeControl.setDevice(getDevice());
			controls.getChildren().add(gameModeControl.getScene().getRoot());
			controllers.add(gameModeControl);
		}

	}

	@Override
	protected void onCleanUp() {
		for (Controller c : controllers)
			c.cleanUp();
		controllers.clear();
	}

	@FXML
	void evtAbout(ActionEvent evt) {
		context.push(About.class, Direction.FADE_IN);
	}

	@FXML
	void evtOptions(ActionEvent evt) {
		context.push(Options.class, Direction.FROM_BOTTOM);
	}

	@FXML
	void evtMacros(ActionEvent evt) {
		context.push(Macros.class, Direction.FROM_BOTTOM);
	}

	public void configure(Lit lit, Class<? extends Effect> configurable) {
	}
}
