package uk.co.bithatch.snake.widgets;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javafx.animation.FadeTransition;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Glow;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;

public class JavaFX {

	static Text helper = new Text();

	/**
	 * Clips the children of the specified {@link Region} to its current size. This
	 * requires attaching a change listener to the region’s layout bounds, as JavaFX
	 * does not currently provide any built-in way to clip children.
	 * 
	 * @param region the {@link Region} whose children to clip
	 * @param arc    the {@link Rectangle#arcWidth} and {@link Rectangle#arcHeight}
	 *               of the clipping {@link Rectangle}
	 * @throws NullPointerException if {@code region} is {@code null}
	 */
	public static void clipChildren(Region region, double arc) {

		final Rectangle outputClip = new Rectangle();
		outputClip.setArcWidth(arc);
		outputClip.setArcHeight(arc);
		region.setClip(outputClip);

		region.layoutBoundsProperty().addListener((ov, oldValue, newValue) -> {
			outputClip.setWidth(newValue.getWidth());
			outputClip.setHeight(newValue.getHeight());
		});
	}

	public static void bindManagedToVisible(Node... node) {
		for (Node n : node) {
			n.managedProperty().bind(n.visibleProperty());
		}
	}

	public static void fadeHide(Node node, float seconds) {
		fadeHide(node, seconds, null);
	}

	public static void fadeHide(Node node, float seconds, EventHandler<ActionEvent> onFinish) {
		FadeTransition anim = new FadeTransition(Duration.seconds(seconds));
		anim.setCycleCount(1);
		anim.setNode(node);
		anim.setFromValue(1);
		anim.setToValue(0);
		anim.play();
		if (onFinish != null)
			anim.onFinishedProperty().set(onFinish);
	}

	public static double computeTextWidth(Font font, String text, double wrappingWidth) {
		helper.setText(text);
		helper.setFont(font);
		helper.setWrappingWidth(0);
		double w = Math.min(helper.prefWidth(-1), wrappingWidth);
		helper.setWrappingWidth((int) Math.ceil(w));
		return Math.ceil(helper.getLayoutBounds().getWidth());
	}

	public static double computeTextHeight(Font font, String text, double wrappingWidth) {
		helper.setText(text);
		helper.setFont(font);
		helper.setWrappingWidth((int) wrappingWidth);
		return helper.getLayoutBounds().getHeight();

	}

	public static void selectFilesDir(FileChooser fileChooser, String path) {
		Path dir = Paths.get(path);
		Path file = dir;
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
		fileChooser.setInitialFileName(file.getFileName().toString());
	}

	public static void size(Region node, double width, double height) {
		node.setPrefHeight(height);
		node.setPrefWidth(width);
		node.setMaxHeight(height);
		node.setMaxWidth(width);
		node.setMinHeight(height);
		node.setMinWidth(width);
		
	}
	public static void sizeToImage(ButtonBase button, double width, double height) {
		int sz = (int) width;
		int df = sz / 8;
		sz -= df;
		if (button.getGraphic() != null) {
			ImageView iv = ((ImageView) button.getGraphic());
			iv.setFitWidth(width - df);
			iv.setFitHeight(height - df);
			iv.setSmooth(true);
		} else {
			int fs = (int) (sz * 0.6f);
			button.setStyle("-fx-font-size: " + fs + "px;");
		}
		button.setMinSize(width, height);
		button.setMaxSize(width, height);
		button.setPrefSize(width, height);
		button.layout();
	}

	public static String toHex(Color color) {
		return toHex(color, false);
	}

	public static String toHex(Color color, boolean opacity) {
		return toHex(color, opacity ? color.getOpacity() : -1);
	}

	public static String toHex(Color color, double opacity) {
		if (opacity > -1)
			return String.format("#%02x%02x%02x%02x", (int) (color.getRed() * 255), (int) (color.getGreen() * 255),
					(int) (color.getBlue() * 255), (int) (opacity * 255));
		else
			return String.format("#%02x%02x%02x", (int) (color.getRed() * 255), (int) (color.getGreen() * 255),
					(int) (color.getBlue() * 255));
	}

	public static Color toColor(int[] color) {
		return new Color((float) color[0] / 255f, (float) color[1] / 255f, (float) color[2] / 255f, 1);
	}

	public static int[] toRGB(Color color) {
		return new int[] { (int) (color.getRed() * 255.0), (int) (color.getGreen() * 255.0),
				(int) (color.getBlue() * 255.0) };
	}

	public static String toHex(int[] rgb) {
		return String.format("#%02x%02x%02x", rgb[0], rgb[1], rgb[2]);
	}

	public static String toCssRGBA(Color color) {
		return String.format("rgba(%f,%f,%f,%f)", color.getRed(), color.getGreen(), color.getBlue(),
				color.getOpacity());
	}

	public static void onAttachedToScene(Node node, Runnable onFinish) {
		ChangeListener<? super Parent> listener = new ChangeListener<>() {
			@Override
			public void changed(ObservableValue<? extends Parent> observable, Parent oldValue, Parent newValue) {
				if (oldValue == null && newValue != null) {
					if (newValue.parentProperty().get() == null) {
						onAttachedToScene(newValue, () -> onFinish.run());
					} else {
						onFinish.run();
					}
					node.parentProperty().removeListener(this);
				}
			}
		};
		node.parentProperty().addListener(listener);
	}

	public static <T> void zoomTo(ListView<T> root, T item) {
		root.getSelectionModel().select(item);
		root.getFocusModel().focus(root.getSelectionModel().getSelectedIndex());
		root.scrollTo(item);

	}

	public static int encodeRGB(int r, int g, int b) {
		return (r & 0xff) << 16 | (g & 0xff) << 8 | b & 0xff;
	}

	public static int encode(Color color) {
		return encodeRGB((int) (color.getRed() * 255.0), (int) (color.getGreen() * 255.0),
				(int) (color.getBlue() * 255.0));
	}

	public static void glowOrDeemphasis(Node node, boolean highlight) {
		if (highlight) {
			node.getStyleClass().remove("deemphasis");
			node.setEffect(new Glow(0.9));
		} else {
			if (!node.getStyleClass().contains("deemphasis"))
				node.getStyleClass().add("deemphasis");
			node.setEffect(null);
		}
	}

	public static Tooltip quickTooltip(String text) {
		Tooltip tt = new Tooltip(text);
		tt.setShowDelay(Duration.millis(250));
		return tt;
	}

}
