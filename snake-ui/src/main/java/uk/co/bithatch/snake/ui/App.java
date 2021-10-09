package uk.co.bithatch.snake.ui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.Stack;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.goxr3plus.fxborderlessscene.borderless.BorderlessScene;
import com.sshtools.forker.wrapped.Wrapped;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import uk.co.bithatch.snake.lib.Backend;
import uk.co.bithatch.snake.lib.Backend.BackendListener;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.DeviceType;
import uk.co.bithatch.snake.ui.Configuration.TrayIcon;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;
import uk.co.bithatch.snake.ui.addons.AddOnManager;
import uk.co.bithatch.snake.ui.addons.Theme;
import uk.co.bithatch.snake.ui.audio.AudioManager;
import uk.co.bithatch.snake.ui.effects.EffectManager;
import uk.co.bithatch.snake.ui.macros.LegacyMacroStorage;
import uk.co.bithatch.snake.ui.macros.MacroManager;
import uk.co.bithatch.snake.ui.tray.Tray;
import uk.co.bithatch.snake.ui.util.Strings;
import uk.co.bithatch.snake.widgets.Direction;
import uk.co.bithatch.snake.widgets.JavaFX;
import uk.co.bithatch.snake.widgets.SlideyStack;

public class App extends Application implements BackendListener {

	static {
		/* For transparent SVG icons (because sadly Swing has to be used to achieve this */
		System.setProperty("swing.jlf.contentPaneTransparent", "true");
	}
	public static int DROP_SHADOW_SIZE = 11;

	public final static Preferences PREFS = Preferences.userNodeForPackage(App.class);

	static ResourceBundle BUNDLE = ResourceBundle.getBundle(App.class.getName());

	final static System.Logger LOG = System.getLogger(App.class.getName());

	private static List<String> argsList;

	public static void boot(Stage stage) throws Exception {
		App app = new App();
		app.start(stage);
	}

	public static File getCustomCSSFile() {
		File tmpFile = new File(new File(System.getProperty("java.io.tmpdir")),
				System.getProperty("user.name") + "-snake-jfx.css");
		return tmpFile;
	}

	public static String getCustomCSSResource(Configuration configuration) {
		StringBuilder bui = new StringBuilder();

		// Get the base colour. All other colours are derived from this
		Color backgroundColour = new Color(0, 0, 0, 1.0 - ((double) configuration.getTransparency() / 100.0));

		if (backgroundColour.getOpacity() == 0) {
			// Prevent total opacity, as mouse events won't be received
			backgroundColour = new Color(backgroundColour.getRed(), backgroundColour.getGreen(),
					backgroundColour.getBlue(), 1f / 255f);
		}

		bui.append("* {\n");

		bui.append("-fx-background: ");
		bui.append(JavaFX.toHex(backgroundColour, true));
		bui.append(";\n");
		bui.append("}\n");
		return bui.toString();

	}

	public static void main(String[] args) throws Exception {
		argsList = Arrays.asList(args);
		launch(args);
	}

	void setColors(Class<? extends Controller> controller, Scene scene) {
		scene.setFill(new Color(0, 0, 0, 0));
		addStylesheets(controller, configuration.getTheme(), scene.getRoot());
	}

	static void writeCSS(Configuration configuration) {
		try {
			File tmpFile = getCustomCSSFile();
			String url = toUri(tmpFile).toExternalForm();
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("Writing user style sheet to %s", url));
			PrintWriter pw = new PrintWriter(new FileOutputStream(tmpFile));
			try {
				pw.println(getCustomCSSResource(configuration));
			} finally {
				pw.close();
			}
		} catch (IOException e) {
			throw new RuntimeException("Could not create custom CSS resource.");
		}
	}

	private static URL toUri(File tmpFile) {
		try {
			return tmpFile.toURI().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private Backend backend;
	private Stack<Controller> controllers = new Stack<>();
	private Scene primaryScene;
	private Stage primaryStage;
	private SlideyStack stackPane;
	private Tray tray;
	private boolean waitingForExitChoice;
	private Window window;
	private List<Backend> backends = new ArrayList<>();
	private AddOnManager addOnManager;
	private Configuration configuration;
	private DeviceLayoutManager layouts;
	private Cache cache;
	private boolean backendInited;
	private LegacyMacroStorage legacyMacroStorage;
	private EffectManager effectManager;
	private MacroManager macroManager;
	private AudioManager audioManager;

	private SchedulerManager schedulerManager;

	public DeviceLayoutManager getLayouts() {
		return layouts;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public Cache getCache() {
		return cache;
	}

	public void close() {
		close(configuration.getTrayIcon() == TrayIcon.OFF);
	}

	public void close(boolean shutdown) {
		if (shutdown) {
			try {
				macroManager.close();
			} catch (Exception e) {
			}
			try {
				effectManager.close();
			} catch (Exception e) {
			}
			layouts.saveAll();
			try {
				getPreferences().flush();
			} catch (BackingStoreException bse) {
				LOG.log(Level.ERROR, "Failed to flush configuration.", bse);
			}

			Platform.runLater(() -> clearControllers());
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, "Shutting down app.");
			try {
				cache.close();
			} catch (Exception e) {
			}
			try {
				tray.close();
			} catch (Exception e) {
			}
			try {
				backend.close();
			} catch (Exception e) {
			}
			try {
				schedulerManager.close();
			} catch (Exception e) {
			}
			try {
				audioManager.close();
			} catch (Exception e) {
			}
			if(Wrapped.isWrapped())
				Wrapped.get().close();
			Platform.exit();
		} else {
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, "Hiding app.");

			Platform.runLater(() -> primaryStage.hide());
		}
	}

	public void editMacros(AbstractDeviceController from) throws Exception {
		Device device = from.getDevice();
		if (macroManager.isSupported(device)) {
			Bank bank = push(Bank.class, from, Direction.FROM_BOTTOM);
			bank.setBank(macroManager.getMacroSystem().getActiveBank(macroManager.getMacroDevice(device)));
		} else if (device.getCapabilities().contains(Capability.MACRO_PROFILES)) {
			MacroMap map = push(MacroMap.class, from, Direction.FROM_BOTTOM);
			map.setMap(device.getActiveProfile().getActiveMap());
		} else if (device.getCapabilities().contains(Capability.MACROS)) {
			/* Legacy method */
			push(Macros.class, from, Direction.FROM_BOTTOM);
		} else
			throw new IllegalStateException("Does not support macros.");
	}

	public EffectManager getEffectManager() {
		return effectManager;
	}

	public LegacyMacroStorage getLegacyMacroStorage() {
		return legacyMacroStorage;
	}

	public Backend getBackend() {
		return backend;
	}

	public Window getWindow() {
		return window;
	}

	public boolean isWaitingForExitChoice() {
		return waitingForExitChoice;
	}

	public void open() {
		if (!Platform.isFxApplicationThread())
			Platform.runLater(() -> open());
		else {
			primaryStage.show();
			primaryStage.toFront();
		}
	}

	public AddOnManager getAddOnManager() {
		return addOnManager;
	}

	public void openDevice(Device device) throws Exception {
		controllers.clear();
		stackPane.getChildren().clear();
		if (backend.getDevices().size() != 1) {
			push(Overview.class, Direction.FADE);
		}
		push(DeviceDetails.class, Direction.FADE).setDevice(device);
	}

	public <C extends Controller> C openScene(Class<C> controller) throws IOException {
		return openScene(controller, null);
	}

	@SuppressWarnings("unchecked")
	public <C extends Controller> C openScene(Class<C> controller, String fxmlSuffix) throws IOException {
		URL resource = controller
				.getResource(controller.getSimpleName() + (fxmlSuffix == null ? "" : fxmlSuffix) + ".fxml");
		FXMLLoader loader = new FXMLLoader();
		try {
			loader.setResources(ResourceBundle.getBundle(controller.getName()));
		} catch (MissingResourceException mre) {
			// Don't care
		}

		Theme theme = configuration.getTheme();
		// loader.setLocation(resource);
		loader.setLocation(theme.getResource("App.css"));
		Parent root = loader.load(resource.openStream());
		Controller controllerInst = (Controller) loader.getController();
		if (controllerInst == null) {
			throw new IOException("Controller not found. Check controller in FXML");
		}
		addStylesheets(controller, theme, root);

		Scene scene = new Scene(root);
		controllerInst.configure(scene, this);
		scene.getRoot().getStyleClass().add("rootPane");
		return (C) controllerInst;
	}

	<C extends Controller> void addStylesheets(Class<C> controller, Theme theme, Parent root) {
		ObservableList<String> ss = root.getStylesheets();
		if (theme.getParent() != null && theme.getParent().length() > 0) {
			Theme parentTheme = addOnManager.getTheme(theme.getParent());
			if (parentTheme == null)
				throw new IllegalStateException(String.format("Parent theme %s does not exist for theme %s.",
						theme.getParent(), theme.getId()));
			ss.add(parentTheme.getResource(App.class.getSimpleName() + ".css").toExternalForm());
		}
		Strings.addIfNotAdded(ss, theme.getResource(App.class.getSimpleName() + ".css").toExternalForm());
		URL controllerCssUrl = controller.getResource(controller.getSimpleName() + ".css");
		if (controllerCssUrl != null)
			Strings.addIfNotAdded(ss, controllerCssUrl.toExternalForm());
		File tmpFile = getCustomCSSFile();
		String url = toUri(tmpFile).toExternalForm();
		ss.remove(url);
		ss.add(url);
	}

	public Controller pop() {
		if (!controllers.isEmpty()) {
			addOnManager.pop(controllers.peek());
			stackPane.pop();
			Controller c = controllers.pop();
			c.cleanUp();
			return c;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <C extends Controller> C push(Class<C> controller, Controller from, Direction direction) {
		if (controllers.size() > 0) {
			Controller c = controllers.peek();

			if (c instanceof Modal) {
				throw new IllegalStateException("Already modal.");
			}

			if (c.getClass().equals(controller)) {
				/* Already on same type, on same device? */
				if (c instanceof AbstractDeviceController && from instanceof AbstractDeviceController) {
					((AbstractDeviceController) c).setDevice(((AbstractDeviceController) from).getDevice());
				}
				if (primaryScene instanceof BorderlessScene) {
					/* TODO: Not totally sure why ... */
					setColors(c.getClass(), primaryScene);
				}
				return (C) c;
			}
		}

		try {
			if (primaryScene instanceof BorderlessScene) {
				/* TODO: Not totally sure why ... */
				setColors(controller, primaryScene);
			}
			C fc = openScene(controller, null);
			controllers.push(fc);
			stackPane.push(direction, fc.getScene().getRoot());
			if (fc instanceof AbstractDeviceController && from instanceof AbstractDeviceController) {
				var device = ((AbstractDeviceController) from).getDevice();
				if (device != null)
					((AbstractDeviceController) fc).setDevice(device);
			}
			addOnManager.push(fc);
			return fc;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to push controller.", e);
		}
	}

	public <C extends Controller> C push(Class<C> controller, Direction direction) {
		return push(controller, controllers.isEmpty() ? null : controllers.peek(), direction);
	}

	public void remove(Controller c) {
		controllers.remove(c);
		stackPane.remove(c.getScene().getRoot());
	}

	public SchedulerManager getSchedulerManager() {
		return schedulerManager;
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		if (Wrapped.isWrapped()) {
			Wrapped.get().addLaunchListener((args) -> {
				open();
				return 0;
			});
		}

		schedulerManager = new SchedulerManager();
		cache = new Cache(this);
		legacyMacroStorage = new LegacyMacroStorage(this);
		layouts = new DeviceLayoutManager(this);
		effectManager = new EffectManager(this);
		addOnManager = new AddOnManager(this);
		configuration = new Configuration(PREFS, this);
		macroManager = new MacroManager(this);
		audioManager = new AudioManager(this);

		setUserAgentStylesheet(STYLESHEET_MODENA);
		writeCSS(configuration);

		Platform.setImplicitExit(configuration.getTrayIcon() == TrayIcon.OFF);
		configuration.getNode().addPreferenceChangeListener((evt) -> {
			Platform.runLater(() -> {
				if (evt.getKey().equals(Configuration.PREF_THEME))
					recreateScene();
				else if (evt.getKey().equals(Configuration.PREF_TRAY_ICON))
					Platform.setImplicitExit(configuration.getTrayIcon() == TrayIcon.OFF);
				else if (evt.getKey().equals(Configuration.PREF_TRANSPARENCY)) {
					writeCSS(configuration);
					setColors(peek().getClass(), primaryScene);
				} else if (evt.getKey().equals(Configuration.PREF_DECORATED))
					recreateScene();
			});

		});

		String activeBackend = PREFS.get("backend", "");
		for (Backend possibleBackend : ServiceLoader.load(Backend.class)) {
			if (activeBackend.equals("") || activeBackend.equals(possibleBackend.getClass().getName())) {
				LOG.log(Level.DEBUG, String.format("Backend %s* available.", possibleBackend.getName()));
				backend = possibleBackend;
			} else
				LOG.log(Level.DEBUG, String.format("Backend %s available.", possibleBackend.getName()));
			backends.add(possibleBackend);
		}
		if (backend == null && !backends.isEmpty())
			backend = backends.get(0);

		// Setup the window
		this.primaryStage = primaryStage;

		createMainScene();

		/* Final configuration of the primary stage (i.e. the desktop window itself) */
		configureStage(primaryStage);

		/* Show! */
		if (!argsList.contains("--no-open"))
			primaryStage.show();

		if (PlatformService.get().isUpdated())
			push(Changes.class, Direction.FROM_TOP);

		/* Autostart by default */
		if (!PREFS.getBoolean("installed", false)) {
			PlatformService.get().setStartOnLogin(true);
			PREFS.putBoolean("installed", true);
		}

		if(Wrapped.isWrapped())
			Wrapped.get().ready(); 
	}

	private void clearControllers() {
		for (Controller c : controllers)
			c.cleanUp();
		controllers.clear();
	}

	private void configureStage(Stage primaryStage) {
		if (configuration.hasBounds()) {
			primaryStage.setX(configuration.getX());
			primaryStage.setY(configuration.getY());
			primaryStage.setWidth(configuration.getW());
			primaryStage.setHeight(configuration.getH());
		}
		primaryStage.xProperty().addListener((e) -> configuration.setX((int) primaryStage.getX()));
		primaryStage.yProperty().addListener((e) -> configuration.setY((int) primaryStage.getY()));
		primaryStage.widthProperty().addListener((e) -> configuration.setW((int) primaryStage.getWidth()));
		primaryStage.heightProperty().addListener((e) -> configuration.setH((int) primaryStage.getHeight()));
		primaryStage.setTitle(BUNDLE.getString("title"));
		primaryStage.getIcons()
				.add(new Image(configuration.getTheme().getResource("icons/app32.png").toExternalForm()));
		primaryStage.getIcons()
				.add(new Image(configuration.getTheme().getResource("icons/app64.png").toExternalForm()));
		primaryStage.getIcons()
				.add(new Image(configuration.getTheme().getResource("icons/app96.png").toExternalForm()));
		primaryStage.getIcons()
				.add(new Image(configuration.getTheme().getResource("icons/app128.png").toExternalForm()));
		primaryStage.getIcons()
				.add(new Image(configuration.getTheme().getResource("icons/app256.png").toExternalForm()));
		primaryStage.getIcons()
				.add(new Image(configuration.getTheme().getResource("icons/app512.png").toExternalForm()));
		primaryStage.onCloseRequestProperty().set(we -> {
			we.consume();
			close();
		});
	}

	private void createMainScene() throws IOException {

		stackPane = new SlideyStack();

		var anchorPane = new AnchorPane(stackPane);
		anchorPane.setPrefSize(700, 500);
		AnchorPane.setTopAnchor(stackPane, 0.0);
		AnchorPane.setBottomAnchor(stackPane, 0.0);
		AnchorPane.setLeftAnchor(stackPane, 0.0);
		AnchorPane.setRightAnchor(stackPane, 0.0);

		/* The main view */
		try {
			if (!backendInited) {
				initBackend();
				
				addOnManager.start();
				effectManager.open();

				new DesktopNotifications(this);
			}

			if (!macroManager.isStarted())
				macroManager.start();
			controllers.clear();
			if (backend.getDevices().size() == 1) {
				push(DeviceDetails.class, Direction.FADE).setDevice(backend.getDevices().get(0));
			} else {
				push(Overview.class, Direction.FADE);
			}

		} catch (Exception e) {
			LOG.log(Level.ERROR, "Failed to initialize.", e);
			Error fc = openScene(Error.class, null);
			fc.setError(e);
			stackPane.getChildren().add(fc.getScene().getRoot());
			controllers.clear();
			controllers.push(fc);
		}

		if (configuration.isDecorated()) {
			if (primaryStage == null) {
				primaryStage = new Stage(StageStyle.DECORATED);
				configureStage(primaryStage);
			} else
				primaryStage.initStyle(StageStyle.DECORATED);
			primaryScene = new Scene(anchorPane);
			setColors(peek().getClass(), primaryScene);
			primaryStage.setScene(primaryScene);
			window = null;
		} else {
			if (primaryStage == null) {
				primaryStage = new Stage(StageStyle.TRANSPARENT);
				configureStage(primaryStage);
			} else
				primaryStage.initStyle(StageStyle.TRANSPARENT);

			window = openScene(Window.class, null);
			window.getContent().setPrefSize(700, 500);
			window.getContent().getChildren().add(anchorPane);

			/*
			 * Create the borderless scene (3rd party library the handles moving and
			 * resizing when UNDECORATED
			 */
			primaryScene = new BorderlessScene(primaryStage, StageStyle.TRANSPARENT, window.getScene().getRoot(), 250,
					250);
			((BorderlessScene) primaryScene).setMoveControl(window.getBar());
			((BorderlessScene) primaryScene).setSnapEnabled(false);
			((BorderlessScene) primaryScene).removeDefaultCSS();


			primaryScene.getRoot().setEffect(new DropShadow());
			((Region) primaryScene.getRoot()).setPadding(new Insets(10, 10, 10, 10));
			primaryScene.setFill(Color.TRANSPARENT);
			
			setColors(peek().getClass(), primaryScene);
			primaryStage.setScene(primaryScene);
		}
	}

	protected void initBackend() throws Exception {
		if (backend == null)
			throw new IllegalStateException(
					"No backend modules available on the classpath or module path. You need at least one backend. For example, snake-backend-openrazer is the default backend.");
		backend.init();
		backend.addListener(this);
		backendInited = true;

		/* The tray */
		tray = new Tray(this);

		/* Activate the selected effect on all devices */
		try {
			for (Device dev : backend.getDevices()) {
				try {
					if (dev.getCapabilities().contains(Capability.MACROS)
							&& !dev.getCapabilities().contains(Capability.MACRO_PROFILES))
						legacyMacroStorage.addDevice(dev);

					effectManager.addDevice(dev);
				} catch (Exception e) {
					LOG.log(Level.ERROR, String.format("Failed to set initial effects fot %s. ", dev.getSerial()), e);
				}
			}
		} catch (Exception e) {
			LOG.log(Level.ERROR, "Failed to set initial effects. Failed to enumerate devices.", e);
		}
	}

	private void recreateScene() {
		try {
			clearControllers();
			primaryStage.close();
			primaryStage = null;
			createMainScene();
			primaryStage.show();
		} catch (Exception e) {
			LOG.log(Level.ERROR, "Failed to create scene.", e);
		}
	}

	public Controller peek() {
		return controllers.peek();
	}

	public Stack<Controller> getControllers() {
		return controllers;
	}

	public Preferences getPreferences() {
		return PREFS;
	}

	public Preferences getPreferences(Device device) {
		return getPreferences().node("devices").node(device.getSerial());
	}

	public Preferences getPreferences(String deviceType) {
		return getPreferences().node("deviceTypes").node(deviceType);
	}

	public String getDefaultImage(DeviceType type, String uri) {
		if (uri == null || uri.equals("")) {
			uri = configuration.getTheme().getResource("devices/" + type.name().toLowerCase() + "512.png")
					.toExternalForm();
		}
		return uri;
	}

	public MacroManager getMacroManager() {
		return macroManager;
	}

	@Override
	public void deviceAdded(Device device) {
	}

	@Override
	public void deviceRemoved(Device device) {
		Platform.runLater(() -> {
			boolean haveDevice = false;
			for (Controller c : controllers) {
				if (c instanceof AbstractDeviceController) {
					if (((AbstractDeviceController) c).getDevice().equals(device)) {
						haveDevice = true;
					}
				}
			}
			if (haveDevice) {
				while (controllers.size() > 1)
					pop();

				Controller peek = controllers.peek();
				if (peek instanceof AbstractDeviceController
						&& ((AbstractDeviceController) peek).getDevice().equals(device)) {
					/* TODO */
					LOG.log(Level.WARNING, "TODO! This device is gone.");
				}
			}
		});
	}

	public AudioManager getAudioManager() {
		return audioManager;
	}

	public static void hideyMessage(App context, Node message) {
		message.visibleProperty().set(true);
		ScheduledFuture<?> task = context.getSchedulerManager().get(Queue.TIMER).schedule(
				() -> Platform
						.runLater(() -> JavaFX.fadeHide(message, 5, (ev) -> message.visibleProperty().set(false))),
				1, TimeUnit.MINUTES);
		message.onMouseClickedProperty().set((e) -> {
			task.cancel(false);
			JavaFX.fadeHide(message, 5, (ev) -> message.visibleProperty().set(false));
		});

	}

}