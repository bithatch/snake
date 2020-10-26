package uk.co.bithatch.snake.updater;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.prefs.Preferences;

import com.sshtools.forker.updater.AppManifest;
import com.sshtools.forker.updater.DesktopShortcut;
import com.sshtools.forker.updater.InstallHandler;
import com.sshtools.forker.updater.InstallSession;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class Install implements Controller, InstallHandler {
	final static ResourceBundle bundle = ResourceBundle.getBundle(Install.class.getName());

	final static Preferences PREFS = Preferences.userRoot().node("uk").node("co").node("bithatch").node("snake")
			.node("ui");
	private static final String SNAKE_RAZER_DESKTOP = "snake-razer";

	@FXML
	private Button browse;
	@FXML
	private Hyperlink install;
	@FXML
	private TextField installLocation;
	@FXML
	private CheckBox installShortcut;
	@FXML
	private CheckBox launch;
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
	private InstallSession session;
	private Bootstrap bootstrap;
	private Callable<Void> callback;
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
	
	@Override
	public Path chooseDestination(Callable<Void> callable) {
		this.callback = callable;
		return null;
	}

	@Override
	public Path chosenDestination() {
		return Paths.get(installLocation.textProperty().get());
	}

	@Override
	public void complete() {
		bootstrap.setInstalled();

		try {
			if (installShortcut.selectedProperty().get()) {
				session.installShortcut(new DesktopShortcut(SNAKE_RAZER_DESKTOP)
						.comment("Control and configure your Razer devices").name("Snake")
						.executable(installLocation.textProperty().get() + File.separator + "bin/snake")
						.addCategories("Utility", "Core").addKeywords("razer", "snake", "mamba", "chroma", "deathadder")
						.icon(Install.class.getResource("appicon/razer-color-512.png").toExternalForm()));
			} else {
				session.uninstallShortcut(SNAKE_RAZER_DESKTOP);
			}
		} catch (IOException ioe) {
			ioe.printStackTrace();
		}
		Platform.runLater(() -> {
			message("success");
			status.getStyleClass().add("success");
		});
		executor.shutdown();
	}

	public void doneDownloads() throws Throwable {
		Platform.runLater(() -> {
			bootstrap.getStage().show();
			message("doneDownloads");
		});
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
	public void init(InstallSession session) {
		this.session = session;

		titleBar.setBackground(new Background(
				new BackgroundImage(new Image(getClass().getResource("titleBar.png").toExternalForm(), true),
						BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
						new BackgroundSize(100d, 100d, true, true, false, true))));

		title.setFont(Bootstrap.font);

		progressContainer.managedProperty().bind(progressContainer.visibleProperty());
		options.managedProperty().bind(options.visibleProperty());
		progressContainer.visibleProperty().set(false);

		installLocation.textProperty().set(PREFS.get("installLocation", session.base().toString()));
		installLocation.textProperty().addListener((e) -> checkInstallable());
		status.textProperty().set(bundle.getString("preparing"));
		progress.setProgress(-1);
		bootstrap.getStage().show();

	}

	@Override
	public void installFile(Path file, Path d) throws Exception {
		Platform.runLater(() -> {
			bootstrap.getStage().show();
			message("installFile", file.getFileName().toString());
		});

	}

	@Override
	public void installFileDone(Path file) throws Exception {
		message("installFileDone", file.getFileName().toString());
	}

	@Override
	public void installFileProgress(Path file, float progress) throws Exception {
	}

	@Override
	public void installProgress(float frac) throws Exception {
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
	public void startInstall() throws Exception {
		Platform.runLater(() -> {
			options.visibleProperty().set(false);
			progressContainer.visibleProperty().set(true);
			bootstrap.getStage().show();
			message("startInstall");
		});
	}

	protected void checkInstallable() {
		Path p = chosenDestination();
		install.disableProperty()
				.set(Files.isRegularFile(p) || (!isExistsAndIsEmpty(p) && Files.exists(p) && !isSameAppId(p)));
	}

	@FXML
	void evtBrowse(ActionEvent evt) {
		DirectoryChooser fileChooser = new DirectoryChooser();
		fileChooser.setTitle(bundle.getString("selectTarget"));
		Path dir = chosenDestination();
		while (dir != null) {
			if (Files.isDirectory(dir)) {
				break;
			}
			dir = dir.getParent();
		}
		if (dir == null)
			fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
		else {
			fileChooser.setInitialDirectory(dir.toFile());
		}
		File file = fileChooser.showDialog((Stage) getScene().getWindow());
		if (file != null) {
			installLocation.textProperty().set(file.getPath());
		}
	}

	@FXML
	void evtClose(ActionEvent evt) {
		bootstrap.close();
	}

	@FXML
	void evtInstall(ActionEvent evt) {
		PREFS.put("installLocation", installLocation.textProperty().get());
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

	private boolean isExistsAndIsEmpty(Path p) {
		if (Files.exists(p) && Files.isDirectory(p)) {
			return p.toFile().list().length == 0;
		}
		return false;
	}

	private boolean isSameAppId(Path p) {
		Path manifest = p.resolve("manifest.xml");
		if (Files.exists(manifest)) {
			try {
				AppManifest m = new AppManifest(manifest);
				return m.id().equals(session.manifest().id());
			} catch (IOException ioe) {
			}
		}
		return false;
	}
}
