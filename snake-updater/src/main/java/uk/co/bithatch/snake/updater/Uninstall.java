package uk.co.bithatch.snake.updater;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.sshtools.forker.updater.UninstallHandler;
import com.sshtools.forker.updater.UninstallSession;
import com.sshtools.forker.updater.Uninstaller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Uninstall implements Controller, UninstallHandler {

	/** The logger. */
	protected Logger logger = Logger.getGlobal();

	private final static ResourceBundle bundle = ResourceBundle.getBundle(Uninstall.class.getName());

	private final static Preferences PREFS = Preferences.userRoot().node("uk").node("co").node("bithatch").node("snake");
	
	private static final String SNAKE_RAZER_DESKTOP = "snake-razer";

	@FXML
	private Hyperlink install;
	@FXML
	private CheckBox deleteAll;
	@FXML
	private BorderPane options;
	@FXML
	private ProgressIndicator progress;
	@FXML
	private VBox progressContainer;
	@FXML
	private Label status;
	@FXML
	private Label title;
	@FXML
	private BorderPane titleBar;

	private Scene scene;
	private UninstallSession session;
	private Bootstrap bootstrap;
	private Callable<Void> callback;
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

	@Override
	public Boolean prep(Callable<Void> callable) {
		this.callback = callable;
		return null;
	}

	@Override
	public Boolean value() {
		return deleteAll.selectedProperty().getValue();
	}

	@Override
	public void uninstallDone() {
	}

	@Override
	public void complete() {
		bootstrap.setInstalled();
		Platform.runLater(() -> {
			message("success");
			status.getStyleClass().add("success");
		});
		executor.shutdown();
	}

	@Override
	public void failed(Throwable t) {
		Platform.runLater(() -> {
			message("failed", t.getMessage() == null ? "No message supplied." : t.getMessage());
			progress.visibleProperty().set(false);
			progress.managedProperty().set(false);
			status.getStyleClass().add("danger");
		});
	}

	@Override
	public Scene getScene() {
		return scene;
	}

	@Override
	public void init(UninstallSession session) {
		this.session = session;

		titleBar.setBackground(new Background(
				new BackgroundImage(new Image(getClass().getResource("titleBar.png").toExternalForm(), true),
						BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
						new BackgroundSize(100d, 100d, true, true, false, true))));

		title.setFont(Bootstrap.font);

		progressContainer.managedProperty().bind(progressContainer.visibleProperty());
		options.managedProperty().bind(options.visibleProperty());
		progressContainer.visibleProperty().set(false);
		
		session.addFile(getShare().resolve("pixmaps").resolve("snake-razer.png"));
		session.addFile(getConf().resolve("autostart").resolve("snake-razer.desktop"));
		session.addDirectory(session.base().resolve("addons"));
		session.addFile(session.base().resolve("app.args"));

		status.textProperty().set(bundle.getString("preparing"));
		progress.setProgress(-1);
		bootstrap.getStage().show();
		
		Uninstaller uninstaller = this.session.tool();
		if (uninstaller != null)
			uninstaller.closeSplash(); 
	}

	@Override
	public void uninstallFile(Path file, Path d, int index) throws Exception {
		Platform.runLater(() -> {
			bootstrap.getStage().show();
			message("uninstallFile", file.getFileName().toString());
		});
	}

	@Override
	public void uninstallFileDone(Path file) throws Exception {
		message("uninstallFileDone", file.getFileName().toString());
	}

	@Override
	public void uninstallFileProgress(Path file, float progress) throws Exception {
	}

	@Override
	public void uninstallProgress(float frac) throws Exception {
		Platform.runLater(() -> progress.progressProperty().set(frac));
	}

	public void setBootstrap(Bootstrap bootstrap) {
		this.bootstrap = bootstrap;
	}

	@Override
	public void setScene(Scene scene) {
		this.scene = scene;
	}

	@Override
	public void startUninstall() throws Exception {
		Platform.runLater(() -> {
			options.visibleProperty().set(false);
			progressContainer.visibleProperty().set(true);
			bootstrap.getStage().show();
			message("startUninstall");
			try {
				session.uninstallShortcut(SNAKE_RAZER_DESKTOP);
			} catch (IOException ioe) {
				ioe.printStackTrace();
			}
			try {
				if (deleteAll.selectedProperty().get()) {
					PREFS.removeNode();
				}
			} catch (BackingStoreException bse) {
				bse.printStackTrace();
			}
		});
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@FXML
	void evtClose(ActionEvent evt) {
		bootstrap.close();
	}

	@FXML
	void evtUninstall(ActionEvent evt) {
		new Thread() {
			public void run() {
				try {
					callback.call();
				} catch (Exception e) {
					throw new IllegalStateException("Failed to continue installation.", e);
				}
			}
		}.start();
	}

	@FXML
	void evtMin(ActionEvent evt) {
		Stage stage = (Stage) ((Hyperlink) evt.getSource()).getScene().getWindow();
		stage.setIconified(true);
	}

	void message(String key, String... args) {
		if (Platform.isFxApplicationThread()) {
			String txt = MessageFormat.format(bundle.getString(key), (Object[]) args);
			status.textProperty().set(txt);
		} else
			Platform.runLater(() -> message(key, args));
	}

	Path getShare() {
		return getHome().resolve(".local").resolve("share");
	}

	Path getHome() {
		return Paths.get(System.getProperty("user.home"));
	}

	Path getConf() {
		return getHome().resolve(".config");
	}
}
