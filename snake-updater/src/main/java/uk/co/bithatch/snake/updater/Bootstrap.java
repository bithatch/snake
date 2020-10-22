package uk.co.bithatch.snake.updater;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.goxr3plus.fxborderlessscene.borderless.BorderlessScene;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class Bootstrap extends Application {
	public static Font font;
	static {
		font = Font.loadFont(Install.class.getResource("FROSTBITE-Narrow.ttf").toExternalForm(), 12);
	}

	public static final String REMOTE_CONFIG = "http://blue/repository/config.xml";

	final static ResourceBundle bundle = ResourceBundle.getBundle(Bootstrap.class.getName());

	public static List<Image> images;

	private static Bootstrap instance;

	private Stage primaryStage;
	private StackPane stack;
	private boolean installed;

	{
		instance = this;
	}

	public static void main(String[] args) throws Exception {
		if (instance == null)
			launch(args);
		else {
			instance.relaunch(args);
		}
	}

	private void relaunch(String[] args) {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(() -> relaunch(args));
			return;
		}
		try {
			recreate();
		} catch (Exception e) {
			throw new IllegalStateException("Failed to relaunch.", e);
		}
	}

	@Override
	public void init() {
		System.setProperty("suppress.warning", "true");

		List<String> sizes = List.of("32", "64", "96", "128", "256", "512");
		images = sizes.stream().map(s -> ("/uk/co/bithatch/snake/updater/appicon/razer-color-" + s + ".png"))
				.map(s -> getClass().getResource(s).toExternalForm()).map(Image::new).collect(Collectors.toList());
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		this.primaryStage = primaryStage;
		recreate();
	}

	private void recreate() throws IOException {
		Parent node = null;
		Install install = null;
		Update update = null;
		if (JavaFXUpdateHandler.get().isActive()) {
			update = openScene(Update.class);
			update.setBootstrap(this);
			node = update.getScene().getRoot();
		} else {
			install = openScene(Install.class);
			install.setBootstrap(this);
			node = install.getScene().getRoot();
		}

		if (stack == null) {
			stack = new StackPane(node);
			AnchorPane anchor = new AnchorPane(stack);
			AnchorPane.setBottomAnchor(stack, 0d);
			AnchorPane.setLeftAnchor(stack, 0d);
			AnchorPane.setTopAnchor(stack, 0d);
			AnchorPane.setRightAnchor(stack, 0d);
			BorderlessScene primaryScene = new BorderlessScene(primaryStage, StageStyle.TRANSPARENT, anchor, 460, 200);
			((BorderlessScene) primaryScene).setMoveControl(node);
			primaryStage.setScene(primaryScene);
			primaryStage.setResizable(false);
			primaryStage.getIcons().addAll(images);
			primaryStage.setTitle(bundle.getString("title"));
		} else {
			stack.getChildren().clear();
			stack.getChildren().add(node);
		}

		/* This will release the lock and let everything start */
		if (install != null) {
			JavaFXInstallHandler.get().setDelegate(install);
		} else {
			JavaFXUpdateHandler.get().setDelegate(update);
		}
	}

	@SuppressWarnings("unchecked")
	public <C extends Controller> C openScene(Class<C> controller) throws IOException {
		URL resource = controller.getResource(controller.getSimpleName() + ".fxml");
		FXMLLoader loader = new FXMLLoader();
		loader.setResources(ResourceBundle.getBundle(controller.getName()));
		loader.setLocation(resource);
		Parent root = loader.load(resource.openStream());
		C controllerInst = (C) loader.getController();
		if (controllerInst == null) {
			throw new IOException("Controller not found. Check controller in FXML");
		}
		root.getStylesheets().add(controller.getResource(Bootstrap.class.getSimpleName() + ".css").toExternalForm());
		URL controllerCssUrl = controller.getResource(controller.getSimpleName() + ".css");
		if (controllerCssUrl != null)
			root.getStylesheets().add(controllerCssUrl.toExternalForm());

		AwesomeIcons.install(root);
		Scene scene = new Scene(root);
		controllerInst.setScene(scene); 
		scene.getRoot().getStyleClass().add("rootPane");
		return controllerInst;
	}

	public void close() {
		if (!Platform.isFxApplicationThread()) {
			Platform.runLater(() -> close());
			return;
		}
		try {
			primaryStage.close();
		}
		catch(Exception e) {
			// TODO investigate further. Some strange NPE exception coming from JavaFX when
			// the installer exits (no run-on-install).
		}
	}

	public Stage getStage() {
		return primaryStage;
	}

	public boolean isInstalled() {
		return installed;
	}

	public void setInstalled() {
		this.installed = true;
	}
}
