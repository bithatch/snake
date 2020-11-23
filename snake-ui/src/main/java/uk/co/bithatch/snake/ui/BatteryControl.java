package uk.co.bithatch.snake.ui;

import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;

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

	public static String getBatteryIconText(int level) {
		return bundle.getString(getBatteryIcon(level));
	}

	public static String getBatteryIcon(int level) {
		String t;
		if (level < 1) {
			t = "batteryEmpty";
		} else if (level <= 25) {
			t = "batteryQuarter";
		} else if (level <= 50) {
			t = "batteryHalf";
		} else if (level <= 75) {
			t = "batteryThreeQuarters";
		} else {
			t = "batteryFull";
		}
		return bundle.getString(t);
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
		batteryStatus.textProperty().set(getBatteryIcon(level));
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
