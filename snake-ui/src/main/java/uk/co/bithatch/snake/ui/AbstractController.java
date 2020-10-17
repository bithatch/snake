package uk.co.bithatch.snake.ui;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.function.Consumer;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
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

	protected void walkTree(Object node, Consumer<Object> visitor) {
		if (node == null) {
			return;
		}
		visitor.accept(node);
		if (node instanceof TabPane) {
			((TabPane) node).getTabs().forEach(n -> walkTree(n, visitor));
		} else if (node instanceof Tab) {
			walkTree(((Tab) node).getContent(), visitor);
			walkTree(((Tab) node).getGraphic(), visitor);
		} else if (node instanceof Parent) {
			((Parent) node).getChildrenUnmodifiable().forEach(n -> walkTree(n, visitor));
		}
	}
}
