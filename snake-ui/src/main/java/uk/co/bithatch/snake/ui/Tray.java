package uk.co.bithatch.snake.ui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import javax.swing.JMenu;
import javax.swing.JSeparator;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import dorkbox.systemTray.Checkbox;
import dorkbox.systemTray.Entry;
import dorkbox.systemTray.Menu;
import dorkbox.systemTray.MenuItem;
import dorkbox.systemTray.Separator;
import dorkbox.systemTray.SystemTray;
import javafx.application.Platform;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Grouping;
import uk.co.bithatch.snake.lib.Item;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.Backend.BackendListener;
import uk.co.bithatch.snake.lib.Device.Listener;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.ui.Configuration.TrayIcon;
import uk.co.bithatch.snake.ui.SlideyStack.Direction;

public class Tray implements AutoCloseable, BackendListener, Listener {

	final static ResourceBundle bundle = ResourceBundle.getBundle(Tray.class.getName());

	private Configuration cfg;
	private App context;
	private Font font;
	private SystemTray systemTray;
	private List<Entry> menuEntries = new ArrayList<>();

	private Timer timer;

	public Tray(App context) throws Exception {
		this.context = context;

		font = Font.createFont(Font.TRUETYPE_FONT, Tray.class.getResourceAsStream("fontawesome-webfont.ttf"));

		cfg = Configuration.getDefault();
		cfg.trayIconProperty().addListener((e) -> SwingUtilities.invokeLater(() -> adjustTray()));
		cfg.showBatteryProperty().addListener((e) -> SwingUtilities.invokeLater(() -> adjustTray()));
		cfg.whenLowProperty().addListener((e) -> SwingUtilities.invokeLater(() -> adjustTray()));

		context.getBackend().addListener(this);
		for (Device dev : context.getBackend().getDevices()) {
			dev.addListener(this);
		}
		SwingUtilities.invokeLater(() -> adjustTray());

	}

	@Override
	public void close() throws Exception {
		if (systemTray != null) {
			systemTray.shutdown();
			systemTray = null;
		}
	}

	void addDevice(Device device) throws IOException {
		var img = device.getImage();
		if (img.startsWith("http:") || img.startsWith("https:")) {
			var url = new URL(img);
			var path = url.getPath();
			var idx = path.lastIndexOf('/');
			if (idx != -1) {
				path = path.substring(idx + 1);
			}
			var tf = File.createTempFile("snake", path);
			tf.deleteOnExit();
			try (var fos = new FileOutputStream(tf)) {
				try (InputStream is = url.openStream()) {
					is.transferTo(fos);
				}
			}
			img = tf.getAbsolutePath();
		} else if (img.startsWith("file:")) {
			img = img.substring(5);
		}
		var menu = new Menu(device.getName(), img);
		var openDev = new MenuItem(bundle.getString("open"), (e) -> Platform.runLater(() -> {
			context.open();
			context.push(DeviceDetails.class, Direction.FROM_RIGHT).setDevice(device);
		}));
		menu.add(openDev);
		menuEntries.add(openDev);
		if (device.getCapabilities().contains(Capability.MACROS)) {
			var macros = new MenuItem(bundle.getString("macros"), createAwesomeIcon(bundle.getString("macrosIcon"), 32),
					(e) -> {
						Platform.runLater(() -> {
							context.open();
							context.push(Macros.class, Direction.FROM_BOTTOM).setDevice(device);
						});
					});
			menu.add(macros);
			menuEntries.add(macros);
		}
		if (device.getCapabilities().contains(Capability.BRIGHTNESS)) {
			brightnessMenu(menu, device);
		}
		if (device.getCapabilities().contains(Capability.DPI)) {
			dpiMenu(menu, device);
		}
		if (device.getCapabilities().contains(Capability.POLL_RATE)) {
			pollRateMenu(menu, device);
		}
		if (device.getCapabilities().contains(Capability.GAME_MODE)) {
			gameMode(menu, device);
		}
		menu.add(new JSeparator());
		if (!device.getSupportedEffects().isEmpty()) {
			addEffectsMenu(device, menu, device);
			menu.add(new JSeparator());
		}
		boolean sep;
		for (var r : device.getRegions()) {
			var mi = new Menu(bundle.getString("region." + r.getName().name()));
			mi.setImage(Images.getRegionImage(24, r.getName()));
			menu.add(mi);
			sep = false;
			if (r.getCapabilities().contains(Capability.BRIGHTNESS_PER_REGION)) {
				brightnessMenu(mi, r);
				sep = true;
			}
			if (r.getCapabilities().contains(Capability.EFFECT_PER_REGION)) {
				if (sep) {
					mi.add(new JSeparator());
				}
				addEffectsMenu(r, mi, device);
			}
		}
		systemTray.getMenu().add(menu);
		menuEntries.add(menu);
	}

	void addEffectsMenu(Lit lit, Menu menu, Device device) {
		Class<? extends Effect> configurable = null;
		for (Class<? extends Effect> fx : lit.getSupportedEffects()) {
			var mi = new MenuItem(fx.getSimpleName(), (e) -> {
				context.getScheduler().execute(() -> lit.setEffect(lit.createEffect(fx)));
			});
			mi.setImage(Images.getEffectImage(24, fx));
			menu.add(mi);
			if (fx.equals(lit.getEffect().getClass()) && AbstractEffectController.hasController(fx)) {
				configurable = fx;
			}
		}
		Class<? extends Effect> fConfigurable = configurable;
		if (configurable != null) {
			var mi = new MenuItem(bundle.getString("effectOptions"),
					createAwesomeIcon(bundle.getString("effectOptionsIcon"), 32), (e) -> {
						Platform.runLater(() -> {
							context.open();
							var dev = context.push(DeviceDetails.class, Direction.FROM_RIGHT);
							dev.setDevice(device);
							dev.configure(lit, fConfigurable);
						});
					});
			menu.add(mi);
		}
	}

	BufferedImage createAwesomeIcon(String string, int sz) {
		return createAwesomeIcon(string, sz, 100);
	}

	BufferedImage createAwesomeIcon(String string, int sz, int opac) {
		return createAwesomeIcon(string, sz, opac, null, 0);
	}

	BufferedImage createAwesomeIcon(String string, int sz, int opac, Color col, double rot) {
		var bim = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB);
		var graphics = (Graphics2D) bim.getGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		var szf = (int) ((float) sz * 0.75f);
		var fnt = font.deriveFont((float) szf);
		graphics.setFont(fnt);
		if (opac != 100) {
			graphics.setComposite(makeComposite((float) opac / 100.0f));
		}
		var icon = cfg.trayIconProperty().getValue();
		if (col == null) {
			switch (icon) {
			case LIGHT:
				graphics.setColor(Color.WHITE);
				break;
			case DARK:
				graphics.setColor(Color.BLACK);
				break;
			default:
				// TODO
				break;
			}
		} else {
			graphics.setColor(col);
		}
		graphics.translate(sz / 2, sz / 2);
		graphics.rotate(Math.toRadians(rot));
		graphics.translate(-(graphics.getFontMetrics().stringWidth(string) / 2f),
				(graphics.getFontMetrics().getHeight() / 2f));
		graphics.drawString(string, 0, 0);
		return bim;
	}

	AlphaComposite makeComposite(float alpha) {
		int type = AlphaComposite.SRC_OVER;
		return (AlphaComposite.getInstance(type, alpha));
	}

	void adjustTray() {
		var icon = cfg.trayIconProperty().getValue();
		if (systemTray == null && icon != TrayIcon.OFF) {
			systemTray = SystemTray.get();
			if (systemTray == null) {
				throw new RuntimeException("Unable to load SystemTray!");
			}

			setImage();

			systemTray.setStatus(bundle.getString("title"));
			Menu menu = systemTray.getMenu();
			if (menu == null) {
				systemTray.setMenu(new JMenu(bundle.getString("title")));
			}

			rebuildMenu();

		} else if (systemTray != null && icon == TrayIcon.OFF) {
			systemTray.setEnabled(false);
		} else if (systemTray != null) {
			systemTray.setEnabled(true);
			setImage();
			rebuildMenu();
		}
	}

	private void rebuildMenu() {
		clearMenus();
		var menu = systemTray.getMenu();
		try {

			var mi = new MenuItem(bundle.getString("open"), (e) -> context.open());
			menuEntries.add(mi);
			menu.add(mi);
			addSeparator(menu);

			var backend = context.getBackend();
			var globals = false;
			if (backend.getCapabilities().contains(Capability.BRIGHTNESS)) {
				brightnessMenu(menu, backend);
				globals = true;
			}
			if (backend.getCapabilities().contains(Capability.GAME_MODE)) {
				gameMode(menu, backend);
				globals = true;
			}
			if (globals)
				addSeparator(menu);

			boolean devices = false;
			for (Device dev : context.getBackend().getDevices()) {
				addDevice(dev);
				devices = true;
			}
			if (devices)
				addSeparator(menu);
		} catch (Exception e) {
			// TODO add error item / tooltip?
			systemTray.setTooltip("Erro!");
			e.printStackTrace();
		}

		var quit = new MenuItem(bundle.getString("quit"), (e) -> context.close(true));
		menuEntries.add(quit);
		menu.add(quit).setShortcut('q');
	}

	private void addSeparator(Menu menu) {
		Separator sep = new Separator();
		menu.add(sep);
		menuEntries.add(sep);

	}

	private void clearMenus() {
		var menu = systemTray.getMenu();
		for (Entry dev : menuEntries) {
			menu.remove(dev);
		}
		menuEntries.clear();
	}

	private void gameMode(Menu menu, Grouping backend) {

		Checkbox checkbox = new Checkbox();
		checkbox.setCallback((e) -> backend.setGameMode(checkbox.getChecked()));
		checkbox.setEnabled(true);
		checkbox.setChecked(backend.isGameMode());
		checkbox.setShortcut('g');
		checkbox.setText(bundle.getString("gameMode"));

		menu.add(checkbox);
		menuEntries.add(checkbox);
	}

	private void brightnessMenu(Menu menu, Item backend) {
		var bri = new Menu(bundle.getString("brightness"), createAwesomeIcon(bundle.getString("brightnessIcon"), 32));
		menu.add(bri);
		for (String b : new String[] { "off", "10", "20", "50", "75", "100" }) {
			bri.add(new MenuItem(bundle.getString("brightness." + b),
					createAwesomeIcon(
							b.equals("off") ? bundle.getString("offIcon") : bundle.getString("brightnessIcon"), 32,
							b.equals("off") ? 25 : Integer.parseInt(b)),
					(e) -> {
						backend.setBrightness(b.equals("off") ? 0 : Short.parseShort(b));
					}));
		}
		menuEntries.add(bri);
	}

	private void dpiMenu(Menu menu, Device device) {
		var dpiMenu = new Menu(bundle.getString("dpi"), createAwesomeIcon(bundle.getString("dpiIcon"), 32));
		menu.add(dpiMenu);
		short[] dpis = new short[] { 100, 200, 300, 400, 500, 600, 700, 800, 900, 1000, 1500, 2000, 3000, 4000, 8000,
				16000, 32000 };
		for (short dpi : dpis) {
			if (dpi <= device.getMaxDPI()) {
				dpiMenu.add(new MenuItem(MessageFormat.format(bundle.getString("deviceDpi"), dpi), (e) -> {
					device.setDPI(dpi, dpi);
				}));
			}
		}
		menuEntries.add(dpiMenu);
	}

	private void pollRateMenu(Menu menu, Device device) {
		var pollRateMenu = new Menu(bundle.getString("pollRate"),
				createAwesomeIcon(bundle.getString("pollRateIcon"), 32));
		menu.add(pollRateMenu);
		int[] pollRates = new int[] { 125, 500, 1000 };
		for (int pollRate : pollRates) {
			pollRateMenu.add(new MenuItem(MessageFormat.format(bundle.getString("devicePollRate"), pollRate), (e) -> {
				device.setPollRate(pollRate);
			}));
		}
		menuEntries.add(menu);
	}

	private void setImage() {
		var icon = cfg.trayIconProperty().getValue();
		if (context.getBackend().getCapabilities().contains(Capability.BATTERY) && cfg.showBatteryProperty().getValue()
				&& (!cfg.whenLowProperty().getValue() || (cfg.whenLowProperty().getValue()
						&& context.getBackend().getBattery() <= context.getBackend().getLowBatteryThreshold()))) {
			Color col = null;
			String status = BatteryControl.getBatteryStyle(context.getBackend().getLowBatteryThreshold(),
					context.getBackend().getBattery());
			if ("danger".equals(status))
				col = Color.red;
			else if ("warning".equals(status))
				col = Color.orange;
			systemTray.setImage(createAwesomeIcon(BatteryControl.getBatteryIcon(context.getBackend().getBattery()), 32,
					100, col, 270));
		} else {
			switch (icon) {
			case LIGHT:
				systemTray.setImage(App.class.getResource("appicon/razer-light-128.png"));
				break;
			case DARK:
				systemTray.setImage(App.class.getResource("appicon/razer-dark-128.png"));
				break;
			case AUTO:
				// TODO
			case COLOR:
			default:
				systemTray.setImage(App.class.getResource("appicon/razer-color-128.png"));
				break;
			}
		}
	}

	@Override
	public void deviceAdded(Device device) {
		device.addListener(this);
		SwingUtilities.invokeLater(() -> rebuildMenu());
	}

	@Override
	public void deviceRemoved(Device device) {
		device.removeListener(this);
		SwingUtilities.invokeLater(() -> rebuildMenu());
	}

	@Override
	public void changed(Device device, Region region) {
		SwingUtilities.invokeLater(() -> {
			if (timer == null) {
				timer = new Timer(500, (e) -> {
					rebuildMenu();
				});
				timer.setRepeats(false);
			}
			timer.restart();
		});
	}

}
