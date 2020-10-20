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
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import uk.co.bithatch.snake.lib.Backend;
import uk.co.bithatch.snake.ui.Configuration.TrayIcon;
import uk.co.bithatch.snake.ui.SlideyStack.Direction;

public class App extends Application {
	public static int DROP_SHADOW_SIZE = 11;

	public final static Preferences PREFS = Preferences.userNodeForPackage(App.class);

	static ResourceBundle BUNDLE = ResourceBundle.getBundle(App.class.getName());

	final static System.Logger LOG = System.getLogger(App.class.getName());

	private static List<String> argsList;
	private ScheduledExecutorService scheduler;
	private List<Backend> backends = new ArrayList<>();

	public static void boot(Stage stage) throws Exception {
		App app = new App();
		app.start(stage);
	}

	public static File getCustomCSSFile() {
		File tmpFile = new File(new File(System.getProperty("java.io.tmpdir")),
				System.getProperty("user.name") + "-snake-jfx.css");
		return tmpFile;
	}

	public static String getCustomCSSResource() {
		StringBuilder bui = new StringBuilder();

		// Get the base colour. All other colours are derived from this
		Configuration cfg = Configuration.getDefault();
		Color backgroundColour = new Color(0, 0, 0, 1.0 - ((double) cfg.transparencyProperty().get() / 100.0));

		if (backgroundColour.getOpacity() == 0) {
			// Prevent total opacity, as mouse events won't be received
			backgroundColour = new Color(backgroundColour.getRed(), backgroundColour.getGreen(),
					backgroundColour.getBlue(), 1f / 255f);
		}

		bui.append("* {\n");

		bui.append("-fx-background: ");
		bui.append(UIHelpers.toHex(backgroundColour, true));
		bui.append(";\n");
		bui.append("}\n");
		return bui.toString();

	}

	public static void main(String[] args) throws Exception {
		argsList = Arrays.asList(args);
		launch(args);
	}

	public static void setColors(Class<? extends Controller> controller, Scene scene) {
		scene.setFill(new Color(0, 0, 0, 0));
		addStylesheets(controller, Configuration.getDefault().themeProperty().getValue(), scene.getRoot());

	}

	public static void writeCSS() {
		try {
			File tmpFile = getCustomCSSFile();
			String url = toUri(tmpFile).toExternalForm();
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.DEBUG, String.format("Writing user style sheet to %s", url));
			PrintWriter pw = new PrintWriter(new FileOutputStream(tmpFile));
			try {
				pw.println(getCustomCSSResource());
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

	private boolean backendInited;

	public void clearLoadQueue() {
		loadQueue.shutdownNow();
		loadQueue = Executors.newSingleThreadExecutor();
	}

	public void close() {
		close(Configuration.getDefault().trayIconProperty().getValue() == TrayIcon.OFF);
	}

	public void close(boolean shutdown) {
		if (!Platform.isFxApplicationThread())
			Platform.runLater(() -> close(shutdown));
		else {
			if (shutdown) {
				clearControllers();
				if (LOG.isLoggable(Level.DEBUG))
					LOG.log(Level.DEBUG, "Shutting down app.");
				scheduler.shutdown();
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
				primaryStage.hide();
			}
		}
	}

	public Backend getBackend() {
		return backend;
	}

	public ExecutorService getLoadQueue() {
		return loadQueue;
	}

	public SlideyStack getStack() {
		return stackPane;
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

	public <C extends Controller> C openScene(Class<C> controller) throws IOException {
		return openScene(controller, null);
	}

	@SuppressWarnings("unchecked")
	public <C extends Controller> C openScene(Class<C> controller, String fxmlSuffix) throws IOException {
		URL resource = controller
				.getResource(controller.getSimpleName() + (fxmlSuffix == null ? "" : fxmlSuffix) + ".fxml");
		FXMLLoader loader = new FXMLLoader();
		loader.setResources(ResourceBundle.getBundle(controller.getName()));
		Theme theme = Configuration.getDefault().themeProperty().getValue();
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

	public static <C extends Controller> void addStylesheets(Class<C> controller, Theme theme, Parent root) {
		ObservableList<String> ss = root.getStylesheets();
		if (theme.getParent() != null && theme.getParent().length() > 0) {
			Theme parentTheme = Theme.getTheme(theme.getParent());
			if (parentTheme == null)
				throw new IllegalStateException(String.format("Parent theme %s does not exist for theme %s.", theme.getParent(), theme.getId()));
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

	public void pop() {
		if (!controllers.isEmpty()) {
			stackPane.pop();
			Controller c = controllers.pop();
			c.cleanUp();
		}
	}

	@SuppressWarnings("unchecked")
	public <C extends Controller> C push(Class<C> controller, Controller from, Direction direction) {
		if (controllers.size() > 0) {
			Controller c = controllers.peek();
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
			C fc = openScene(controller, null);
			if (fc instanceof AbstractDeviceController && from instanceof AbstractDeviceController) {
				var device = ((AbstractDeviceController) from).getDevice();
				if (device != null)
					((AbstractDeviceController) fc).setDevice(device);
			}
			stackPane.push(direction, fc.getScene().getRoot());
			controllers.push(fc);
			if (primaryScene instanceof BorderlessScene) {
				/* TODO: Not totally sure why ... */
				setColors(controller, primaryScene);
			}
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

		setUserAgentStylesheet(STYLESHEET_MODENA);
		writeCSS();

		Configuration cfg = Configuration.getDefault();
		Platform.setImplicitExit(cfg.trayIconProperty().getValue() == TrayIcon.OFF);
		cfg.themeProperty().addListener((e) -> recreateScene());
		cfg.trayIconProperty()
				.addListener((e) -> Platform.setImplicitExit(cfg.trayIconProperty().getValue() == TrayIcon.OFF));

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
		cfg.transparencyProperty().addListener((o, oldVal, newVal) -> {
			writeCSS();
			setColors(peek().getClass(), primaryScene);
		});
		cfg.decoratedProperty().addListener((o, oldVal, newVal) -> {
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
	}

	private void clearControllers() {
		for (Controller c : controllers)
			c.cleanUp();
		controllers.clear();
	}

	private void configureStage(Stage primaryStage) {
		Configuration cfg = Configuration.getDefault();
		if (cfg.hasBounds()) {
			primaryStage.setX(cfg.xProperty().get());
			primaryStage.setY(cfg.yProperty().get());
			primaryStage.setWidth(cfg.wProperty().get());
			primaryStage.setHeight(cfg.hProperty().get());
		}
		primaryStage.xProperty().addListener((e) -> cfg.xProperty().set((int) primaryStage.getX()));
		primaryStage.yProperty().addListener((e) -> cfg.yProperty().set((int) primaryStage.getY()));
		primaryStage.widthProperty().addListener((e) -> cfg.wProperty().set((int) primaryStage.getWidth()));
		primaryStage.heightProperty().addListener((e) -> cfg.hProperty().set((int) primaryStage.getHeight()));
		primaryStage.setTitle(BUNDLE.getString("title"));
		primaryStage.getIcons().add(new Image(App.class.getResourceAsStream("appicon/razer-color-512.png")));
		primaryStage.getIcons().add(new Image(App.class.getResourceAsStream("appicon/razer-color-256.png")));
		primaryStage.getIcons().add(new Image(App.class.getResourceAsStream("appicon/razer-color-128.png")));
		primaryStage.getIcons().add(new Image(App.class.getResourceAsStream("appicon/razer-color-96.png")));
		primaryStage.getIcons().add(new Image(App.class.getResourceAsStream("appicon/razer-color-64.png")));
		primaryStage.getIcons().add(new Image(App.class.getResourceAsStream("appicon/razer-color-32.png")));
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
				if (backend == null)
					throw new IllegalStateException(
							"No backend modules available on the classpath or module path. You need at least one backend. For example, snake-backend-openrazer is the default backend.");
				backend.init();
				backendInited = true;

				/* The tray */
				tray = new Tray(this);
			}

			if (backend.getDevices().size() == 1) {
				DeviceDetails details = openScene(DeviceDetails.class);
				details.setDevice(backend.getDevices().get(0));
				stackPane.getChildren().add(details.getScene().getRoot());
				controllers.clear();
				controllers.push(details);
			} else {
				Overview fc = openScene(Overview.class, null);
				stackPane.getChildren().add(fc.getScene().getRoot());
				controllers.clear();
				controllers.push(fc);
			}
		} catch (Exception e) {
			Error fc = openScene(Error.class, null);
			fc.setError(e);
			stackPane.getChildren().add(fc.getScene().getRoot());
			controllers.clear();
			controllers.push(fc);
		}

		if (Configuration.getDefault().decoratedProperty().getValue()) {
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

}
