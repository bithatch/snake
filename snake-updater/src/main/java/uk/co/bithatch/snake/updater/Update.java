package uk.co.bithatch.snake.updater;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.sshtools.forker.updater.Entry;
import com.sshtools.forker.updater.UpdateHandler;
import com.sshtools.forker.updater.UpdateSession;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Scene;
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
import javafx.stage.Stage;

public class Update implements Controller, UpdateHandler {

	final static ResourceBundle bundle = ResourceBundle.getBundle(Update.class.getName());
	
	@FXML
	private ProgressIndicator progress;
	@FXML
	private Label status;
	@FXML
	private Label title;
	@FXML
	private BorderPane titleBar;

	private Bootstrap bootstrap;
	private ScheduledFuture<?> checkTask;
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);
	private Scene scene;

	@Override
	public void complete() {
		Platform.runLater(() -> {
			message("success");
			status.getStyleClass().add("success");
		});
		executor.shutdown();
		bootstrap.close();
	}

	public void doneCheckUpdateFile(Entry file, boolean requires) throws Throwable {
		message("doneCheckUpdateFile", file.path().getFileName().toString());
	}

	public void doneCheckUpdates() throws Throwable {
		message("doneCheckUpdates");
	}

	@Override
	public void doneDownloadFile(Entry file) throws Exception {
	}

	public void doneDownloadFile(Entry file, Path path) throws Throwable {
		message("doneDownloadFile", file.path().getFileName().toString());
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
			status.getStyleClass().add("danger");
		});
	}

	@Override
	public Scene getScene() {
		return scene;
	}

	@Override
	public void init(UpdateSession session) {

		titleBar.setBackground(new Background(
				new BackgroundImage(new Image(getClass().getResource("titleBar.png").toExternalForm(), true),
						BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER,
						new BackgroundSize(100d, 100d, true, true, false, true))));

		title.setFont(Bootstrap.font);

		status.textProperty().set(bundle.getString("check"));
		progress.setProgress(-1);

		if (bootstrap.isInstalled()) {
			Platform.runLater(() -> bootstrap.getStage().show());
		} else {
			/*
			 * Don't show the stage just yet, instead start the check process in the
			 * background, and only show the stage if it is taking more that a few seconds.
			 * This will prevent the update window being shown in most cases (except people
			 * with slow networks to the update server)
			 */
			if (session.tool().getConfiguration().getProperty("update-on-exit") == null) {
				checkTask = executor.schedule(() -> {
					Platform.runLater(() -> bootstrap.getStage().show());
				}, 5, TimeUnit.SECONDS);
			}
		}
	}

	@Override
	public boolean noUpdates(Callable<Void> task) {
		if (checkTask != null)
			checkTask.cancel(false);
		Platform.runLater(() -> {
			status.textProperty().set(bundle.getString("noUpdates"));
			status.getStyleClass().add("success");
		});
		return true;
	}

	public void ready() {
		if (checkTask != null)
			checkTask.cancel(false);
		Platform.runLater(() -> bootstrap.getStage().show());
		status.textProperty().set(bundle.getString("ready"));
	}

	public void setBootstrap(Bootstrap bootstrap) {
		this.bootstrap = bootstrap;
	}

	@Override
	public void setScene(Scene scene) {
		this.scene = scene;
	}

	public void startCheckUpdateFile(Entry file) throws Throwable {
		message("startCheckUpdateFile", file.path().getFileName().toString());
	}

	public void startCheckUpdates() throws Throwable {
		message("startCheckUpdates");
	}

	@Override
	public void startDownloadFile(Entry file, int index) throws Exception {
		Platform.runLater(() -> {
			bootstrap.getStage().show();
			message("startDownloadFile", file.path().getFileName().toString());
		});
	}

	@Override
	public void updateDone(boolean upgradeError) {
	}

	@Override
	public void startDownloads() throws Exception {
		Platform.runLater(() -> {
			bootstrap.getStage().show();
			message("startDownloads");
		});
	}

	public void updateCheckUpdatesProgress(float frac) throws Throwable {
		Platform.runLater(() -> progress.progressProperty().set(frac));
	}

	@Override
	public void updateDownloadFileProgress(Entry file, float frac) throws Exception {
	}

	@Override
	public void updateDownloadProgress(float frac) throws Exception {
		Platform.runLater(() -> progress.progressProperty().set(frac));
	}

	public void validatingFile(Entry file, Path path) throws Throwable {
		message("validatingFile", file.path().getFileName().toString());
	}

	@FXML
	void evtClose(ActionEvent evt) {
		bootstrap.close();
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

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public void startUpdateRollback() {
		Platform.runLater(() -> {
			bootstrap.getStage().show();
			Platform.runLater(() -> progress.progressProperty().set(0));
			message("startUpdateRollback");
		});
	}

	@Override
	public void updateRollbackProgress(float frac) {
		Platform.runLater(() -> {
			bootstrap.getStage().show();
			Platform.runLater(() -> progress.progressProperty().set(frac));
			message("updateRollbackProgress");
		});
		
	}

	@Override
	public Void prep(Callable<Void> callback) {
		return null;
	}

	@Override
	public Void value() {
		return null;
	}
}
