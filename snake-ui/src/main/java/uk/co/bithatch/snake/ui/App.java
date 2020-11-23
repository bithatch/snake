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
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.goxr3plus.fxborderlessscene.borderless.BorderlessScene;
import com.sshtools.forker.wrapped.Wrapped;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import uk.co.bithatch.snake.lib.Backend;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.DeviceType;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.effects.Off;
import uk.co.bithatch.snake.ui.Configuration.TrayIcon;
import uk.co.bithatch.snake.ui.addons.AddOnManager;
import uk.co.bithatch.snake.ui.addons.Theme;
import uk.co.bithatch.snake.ui.effects.EffectAcquisition;
import uk.co.bithatch.snake.ui.effects.EffectManager;
import uk.co.bithatch.snake.ui.util.JavaFX;
import uk.co.bithatch.snake.ui.widgets.Direction;
import uk.co.bithatch.snake.ui.widgets.SlideyStack;

public class App extends Application {

	static {
		/* Need to load once so themes can see. TODO move this to theme system */
		Font.loadFont(AwesomeIcons.class.getResource("fontawesome-webfont.ttf").toExternalForm(), 12);
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
		Color backgroundColour = new Color(0, 0, 0,
				1.0 - ((double) configuration.transparencyProperty().get() / 100.0));

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
		addStylesheets(controller, configuration.themeProperty().getValue(), scene.getRoot());
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
	private ExecutorService loadQueue = Executors.newSingleThreadExecutor();
	private Scene primaryScene;
	private Stage primaryStage;
	private SlideyStack stackPane;
	private Tray tray;
	private boolean waitingForExitChoice;
	private Window window;
	private ScheduledExecutorService scheduler;
	private List<Backend> backends = new ArrayList<>();
	private AddOnManager addOnManager;
	private Configuration configuration;
	private DeviceLayoutManager layouts;
	private Cache cache;
	private boolean backendInited;

	private EffectManager effectManager;

	public void clearLoadQueue() {
		loadQueue.shutdownNow();
		loadQueue = Executors.newSingleThreadExecutor();
	}

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
		close(configuration.trayIconProperty().getValue() == TrayIcon.OFF);
	}

	public void close(boolean shutdown) {
		if (shutdown) {
			if (configuration.turnOffOnExit().getValue()) {
				try {
					for (Device dev : backend.getDevices()) {
						dev.setEffect(new Off());
					}
				} catch (Exception e) {
				}
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
			scheduler.shutdownNow();
			loadQueue.shutdownNow();
			try {
				tray.close();
			} catch (Exception e) {
			}
			try {
				backend.close();
			} catch (Exception e) {
			}
			Platform.exit();
		} else {
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, "Hiding app.");

			Platform.runLater(() -> primaryStage.hide());
		}
	}

	public EffectManager getEffectManager() {
		return effectManager;
	}

	public Backend getBackend() {
		return backend;
	}

	public ExecutorService getLoadQueue() {
		return loadQueue;
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

		Theme theme = configuration.themeProperty().getValue();
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
		ss.add(theme.getResource(App.class.getSimpleName() + ".css").toExternalForm());
		URL controllerCssUrl = controller.getResource(controller.getSimpleName() + ".css");
		if (controllerCssUrl != null)
			ss.add(controllerCssUrl.toExternalForm());
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

	public ScheduledExecutorService getScheduler() {
		return scheduler;
	}

	@Override
	public void start(Stage primaryStage) throws Exception {

		if (Wrapped.isWrapped()) {
			Wrapped.get().addLaunchListener((args) -> {
				open();
				return 0;
			});
		}

		scheduler = Executors.newScheduledThreadPool(1);
		cache = new Cache(this);
		layouts = new DeviceLayoutManager(this);
		effectManager = new EffectManager(this);
		addOnManager = new AddOnManager(this);
		configuration = new Configuration(PREFS, this);

		setUserAgentStylesheet(STYLESHEET_MODENA);
		writeCSS(configuration);

		Platform.setImplicitExit(configuration.trayIconProperty().getValue() == TrayIcon.OFF);
		configuration.themeProperty().addListener((e) -> recreateScene());
		configuration.trayIconProperty().addListener(
				(e) -> Platform.setImplicitExit(configuration.trayIconProperty().getValue() == TrayIcon.OFF));

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

		/* Listen for configuration changes and update UI accordingly */
		configuration.transparencyProperty().addListener((o, oldVal, newVal) -> {
			writeCSS(configuration);
			setColors(peek().getClass(), primaryScene);
		});
		configuration.decoratedProperty().addListener((o, oldVal, newVal) -> {
			recreateScene();
		});

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

		addOnManager.start();
	}

	private void clearControllers() {
		for (Controller c : controllers)
			c.cleanUp();
		controllers.clear();
	}

	private void configureStage(Stage primaryStage) {
		if (configuration.hasBounds()) {
			primaryStage.setX(configuration.xProperty().get());
			primaryStage.setY(configuration.yProperty().get());
			primaryStage.setWidth(configuration.wProperty().get());
			primaryStage.setHeight(configuration.hProperty().get());
		}
		primaryStage.xProperty().addListener((e) -> configuration.xProperty().set((int) primaryStage.getX()));
		primaryStage.yProperty().addListener((e) -> configuration.yProperty().set((int) primaryStage.getY()));
		primaryStage.widthProperty().addListener((e) -> configuration.wProperty().set((int) primaryStage.getWidth()));
		primaryStage.heightProperty().addListener((e) -> configuration.hProperty().set((int) primaryStage.getHeight()));
		primaryStage.setTitle(BUNDLE.getString("title"));
		primaryStage.getIcons().add(
				new Image(configuration.themeProperty().getValue().getResource("icons/app32.png").toExternalForm()));
		primaryStage.getIcons().add(
				new Image(configuration.themeProperty().getValue().getResource("icons/app64.png").toExternalForm()));
		primaryStage.getIcons().add(
				new Image(configuration.themeProperty().getValue().getResource("icons/app96.png").toExternalForm()));
		primaryStage.getIcons().add(
				new Image(configuration.themeProperty().getValue().getResource("icons/app128.png").toExternalForm()));
		primaryStage.getIcons().add(
				new Image(configuration.themeProperty().getValue().getResource("icons/app256.png").toExternalForm()));
		primaryStage.getIcons().add(
				new Image(configuration.themeProperty().getValue().getResource("icons/app512.png").toExternalForm()));
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
			}

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

		if (configuration.decoratedProperty().getValue()) {
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
			setColors(peek().getClass(), primaryScene);
			primaryStage.setScene(primaryScene);
		}
	}

	protected void initBackend() throws Exception {
		if (backend == null)
			throw new IllegalStateException(
					"No backend modules available on the classpath or module path. You need at least one backend. For example, snake-backend-openrazer is the default backend.");
		backend.init();
		backendInited = true;

		/* The tray */
		tray = new Tray(this);

		/* Activate the selected effect on all devices */
		try {
			for (Device dev : backend.getDevices()) {
				try {
					
					/* Acquire an effects controller for this device */
					EffectAcquisition acq = effectManager.acquire(dev);
					
					EffectHandler<?, ?> deviceEffect = acq.getEffect(dev);
					boolean activated = false;
					if (deviceEffect != null && deviceEffect.isMatrixBased()) {
						/* Always activates at device level */
						acq.activate(dev, deviceEffect);
						activated = true;
					} else {
						/* No effect configured for device as a whole, check the regions */
						for (Region r : dev.getRegions()) {
							EffectHandler<?, ?> regionEffect = acq.getEffect(r);
							if (regionEffect != null) {
								acq.activate(r, regionEffect);
								activated = true;
							}
						}
					}

					/* Now try at device level */
					if (!activated && deviceEffect != null) {
						acq.activate(dev, deviceEffect);
						activated = true;
					}

					if (!activated) {
						/* Get the first effect and activate on whole device */
						Set<EffectHandler<?, ?>> effects = effectManager.getEffects(dev);
						if (!effects.isEmpty()) {
							acq.activate(dev, effects.iterator().next());
						}
					}
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
		if(uri == null || uri.equals("")) {
			uri = configuration.themeProperty().getValue().getResource("devices/" + type.name().toLowerCase() + "512.png").toExternalForm();
		}
		return uri;
	}

}
