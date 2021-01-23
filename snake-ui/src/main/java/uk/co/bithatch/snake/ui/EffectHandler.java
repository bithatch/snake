package uk.co.bithatch.snake.ui;

import java.net.URL;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.lib.effects.Matrix;

public interface EffectHandler<E, C extends AbstractEffectController<E, ?>> {

	default String getName() {
		return getClass().getSimpleName();
	}

	default boolean isMatrixBased() {
		return Matrix.class.equals(getBackendEffectClass());
	}

	Class<? extends Effect> getBackendEffectClass();

	Device getDevice();
	
	default boolean isRegions() {
		return !isMatrixBased();
	}

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
		URL effectImage = getEffectImage(size);
		if (effectImage == null)
			return new Label("Missing image " + getClass().getSimpleName());
		else {
			ImageView iv = new ImageView(effectImage.toExternalForm());
			iv.setFitHeight(viewSize);
			iv.setFitWidth(viewSize);
			iv.setSmooth(true);
			iv.setPreserveRatio(true);
			return iv;
		}
	}

	String getDisplayName();

	E activate(Lit component);

	default void deactivate(Lit component) {
	}

	void select(Lit component);

	App getContext();

	default boolean isReadOnly() {
		return true;
	}
}
