package uk.co.bithatch.snake.ui;

import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.util.Duration;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Device.Listener;
import uk.co.bithatch.snake.lib.Region;

public class BatteryControl extends ControlController implements Listener {
	@FXML
	private Label batteryStatus;
	@FXML
	private Label percentage;
	@FXML
	private Spinner<Integer> lowThreshold;
	@FXML
	private Spinner<Integer> idleTime;
	@FXML
	private Label charging;

	private FadeTransition chargingAnim;
	private FadeTransition lowAnim;

	final static ResourceBundle bundle = ResourceBundle.getBundle(BatteryControl.class.getName());

	@Override
	protected void onConfigure() throws Exception {

		chargingAnim = createFader(charging);
		lowAnim = createFader(batteryStatus);

		lowThreshold.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100));
		idleTime.setValueFactory(
				new SpinnerValueFactory.IntegerSpinnerValueFactory(0, (int) TimeUnit.MINUTES.toSeconds(5)));
		lowThreshold.valueProperty()
				.addListener((c) -> getDevice().setLowBatteryThreshold(lowThreshold.getValue().byteValue()));
		idleTime.valueProperty().addListener((c) -> getDevice().setIdleTime(idleTime.getValue()));
	}

	@Override
	protected void onSetControlDevice() {
		Device dev = getDevice();
		setBatteryDevice(dev);
		getDevice().addListener(this);
	}

	public static FadeTransition createFader(Node node) {
		FadeTransition anim = new FadeTransition(Duration.seconds(5));
		anim.setAutoReverse(true);
		anim.setCycleCount(FadeTransition.INDEFINITE);
		anim.setNode(node);
		anim.setFromValue(0.5);
		anim.setToValue(1);
		return anim;
	}

	public static FontAwesome getBatteryIcon(int level) {
		if (level < 1) {
			return FontAwesome.BATTERY_EMPTY;
		} else if (level <= 25) {
			return FontAwesome.BATTERY_QUARTER;
		} else if (level <= 50) {
			return FontAwesome.BATTERY_HALF;
		} else if (level <= 75) {
			return FontAwesome.BATTERY_THREE_QUARTERS;
		} else {
			return FontAwesome.BATTERY_FULL;
		}
	}

	public static String getBatteryStyle(int lowest, int level) {
		int low = (99 - lowest) / 3;
		int medium = low * 2;
		if (level < 1 || level <= lowest) {
			return "danger";
		} else if (level <= low) {
			return "warning";
		} else if (level <= medium) {
			return "success";
		} else {
			return null;
		}
	}

	public static void setBatteryStatusStyle(int lowest, int level, Node node, FadeTransition fader) {
		node.getStyleClass().remove("danger");
		node.getStyleClass().remove("warning");
		node.getStyleClass().remove("success");
		String style = getBatteryStyle(lowest, level);
		if (style == null)
			style = "success";
		if ("danger".equals(style) && fader != null)
			fader.play();
		node.getStyleClass().add(style);
	}

	private void setBatteryDevice(Device dev) {
		int level = dev.getBattery();
		setBatteryStatusStyle(dev.getLowBatteryThreshold(), level, batteryStatus, lowAnim);
		setBatteryStatusStyle(dev.getLowBatteryThreshold(), level, charging, null);
		setBatteryStatusStyle(dev.getLowBatteryThreshold(), level, percentage, null);
		batteryStatus.graphicProperty().set(new FontIcon(getBatteryIcon(level)));
		percentage.textProperty().set(String.format("%d%%", level));
		lowThreshold.getValueFactory().setValue((int) dev.getLowBatteryThreshold());
		idleTime.getValueFactory().setValue(dev.getIdleTime());
		boolean c = dev.isCharging();
		charging.visibleProperty().set(c);
		if (c)
			chargingAnim.play();
		else
			chargingAnim.stop();
	}

	@Override
	protected void onDeviceCleanUp() {
		getDevice().removeListener(this);
		chargingAnim.stop();
	}

	@Override
	public void onChanged(Device device, Region region) {
		setBatteryDevice(device);
	}

}
