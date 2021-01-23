package uk.co.bithatch.snake.ui;

import java.util.Objects;
import java.util.ResourceBundle;

import org.kordamp.ikonli.javafx.FontIcon;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Device.Listener;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.ui.effects.EffectAcquisition;
import uk.co.bithatch.snake.widgets.Direction;

public class DeviceOverview extends AbstractDeviceController implements Listener {
	final static ResourceBundle bundle = ResourceBundle.getBundle(DeviceOverview.class.getName());

	@FXML
	private Label deviceName;
	@FXML
	private Label deviceSerial;
	@FXML
	private Label deviceFirmware;
	@FXML
	private Label deviceStatus;
	@FXML
	private ImageView deviceImage;
	@FXML
	private HBox effect;
	@FXML
	private Label charging;
	@FXML
	private Label battery;
	@FXML
	private Hyperlink macros;
	@FXML
	private Label brightnessAmount;

	private EffectHandler<?, ?> lastEffect;
	private FadeTransition chargingAnim;
	private FadeTransition lowAnim;

	@Override
	protected void onConfigure() throws Exception {
		chargingAnim = BatteryControl.createFader(charging);
		lowAnim = BatteryControl.createFader(battery);
	}

	protected void onSetDevice() {
		Device dev = getDevice();
		dev.addListener(this);
		deviceName.textProperty().set(dev.getName());
		deviceImage.setImage(new Image(
				context.getCache().getCachedImage(context.getDefaultImage(dev.getType(), dev.getImage())), true));
		deviceSerial.textProperty().set(dev.getSerial());
		deviceFirmware.textProperty().set(dev.getFirmware());
		macros.managedProperty().bind(macros.visibleProperty());
		macros.visibleProperty()
				.set(context.getMacroManager().isSupported(dev) || dev.getCapabilities().contains(Capability.MACROS));
		effect.visibleProperty().set(dev.getCapabilities().contains(Capability.EFFECTS));
		battery.visibleProperty().set(dev.getCapabilities().contains(Capability.BATTERY));
		charging.visibleProperty().set(dev.getCapabilities().contains(Capability.BATTERY));
		updateFromDevice(dev);
	}

	private void updateFromDevice(Device dev) {
		EffectAcquisition acq = context.getEffectManager().getRootAcquisition(dev);
		if (acq != null) {
			/* May be null at shutdown */
			EffectHandler<?, ?> thisEffect = acq.getEffect(dev);
			if (!Objects.equals(lastEffect, thisEffect)) {
				lastEffect = thisEffect;
				effect.getChildren().clear();
				if (thisEffect != null)
					effect.getChildren().add(thisEffect.getEffectImageNode(16, 16));
			}
		}
		if (dev.getCapabilities().contains(Capability.BRIGHTNESS)) {
			brightnessAmount.textProperty().set(dev.getBrightness() + "%");
		}
		if (dev.getCapabilities().contains(Capability.BATTERY)) {
			int level = dev.getBattery();
			battery.graphicProperty().set(new FontIcon(BatteryControl.getBatteryIcon(level)));
			boolean c = dev.isCharging();
			charging.visibleProperty().set(c);
			if (c)
				chargingAnim.play();
			else
				chargingAnim.stop();
			BatteryControl.setBatteryStatusStyle(dev.getLowBatteryThreshold(), level, battery, lowAnim);
			BatteryControl.setBatteryStatusStyle(dev.getLowBatteryThreshold(), level, charging, null);
		}
	}

	@FXML
	void evtSelect() {
		context.push(DeviceDetails.class, this, Direction.FROM_RIGHT);
	}

	@FXML
	void evtMacros() {
		try {
			context.editMacros(this);
		} catch (Exception e) {
			error(e);
		}
	}

	@Override
	protected void onDeviceCleanUp() {
		getDevice().removeListener(this);
		chargingAnim.stop();
		lowAnim.stop();
	}

	@Override
	public void onChanged(Device device, Region region) {
		if (region == null)
			updateFromDevice(device);
	}
}
