package uk.co.bithatch.snake.ui;

import java.net.URL;

import javafx.scene.Node;
import javafx.scene.image.ImageView;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;

public interface EffectHandler<E, C extends AbstractEffectController<E, ?>> {

	default String getName() {
		return getClass().getSimpleName();
	}

	default boolean isMatrixBased() {
		return false;
	}

	Device getDevice();

	void open(App context, Device component);

	Class<C> getOptionsController();

	default boolean hasOptions() {
		return getOptionsController() != null;
	}

	default void added(Device device) {
	}

	default void removed(Device device) {
	}

	void store(Lit component, C controller);

	void update(Lit component);

	boolean isSupported(Lit component);

	URL getEffectImage(int size);

	default Node getEffectImageNode(int size, int viewSize) {
		ImageView iv = new ImageView(getEffectImage(size).toExternalForm());
		iv.setFitHeight(viewSize);
		iv.setFitWidth(viewSize);
		iv.setSmooth(true);
		iv.setPreserveRatio(true);
		return iv;
	}

	String getDisplayName();

	void activate(Lit component);

	default void deactivate(Lit component) {
	}

	void select(Lit component);

	App getContext();

	default boolean isReadOnly() {
		return true;
	}
}
