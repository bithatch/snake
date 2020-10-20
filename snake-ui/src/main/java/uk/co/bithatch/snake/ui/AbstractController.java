package uk.co.bithatch.snake.ui;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.scene.Scene;
import javafx.stage.Stage;

public abstract class AbstractController implements Controller {

	final static System.Logger LOG = System.getLogger(AbstractController.class.getName());

	protected App context;
	protected ResourceBundle resources;
	protected URL location;
	protected Scene scene;

	@Override
	public final void initialize(URL location, ResourceBundle resources) {
		this.location = location;
		this.resources = resources;
		onInitialize();
	}

	@Override
	public final void cleanUp() {
		onCleanUp();
	}

	@Override
	public final void configure(Scene scene, App jfxhsClient) {
		this.scene = scene;
		this.context = jfxhsClient;
		try {
			onConfigure();
		} catch (RuntimeException re) {
			throw re;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	protected Stage getStage() {
		return (Stage) scene.getWindow();
	}

	protected void onConfigure() throws Exception {
	}

	protected void onCleanUp() {
	}

	protected void onInitialize() {
	}

	@Override
	public Scene getScene() {
		return scene;
	}
}
