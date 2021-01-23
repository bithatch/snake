package uk.co.bithatch.snake.widgets;

import java.util.List;
//import java.util.ResourceBundle;
import java.util.ResourceBundle;

import com.sshtools.icongenerator.AwesomeIcon;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty; 
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Glow;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;

public class ColorBar extends HBox implements ColorChooser {
	final static ResourceBundle bundle = ResourceBundle.getBundle(ColorBar.class.getName());

	final static Color[] COLORS = new Color[] { Color.RED, Color.ORANGE, Color.YELLOW, Color.GREEN, Color.BLUE,
			Color.INDIGO, Color.VIOLET, Color.WHITE };
	final static String[] COLOR_NAMES = new String[] { bundle.getString("red"), bundle.getString("orange"),
			bundle.getString("yellow"), bundle.getString("green"), bundle.getString("blue"), bundle.getString("indigo"),
			bundle.getString("violet"), bundle.getString("white") };
	

	private Color originalColor = Color.CRIMSON;
	private ObjectProperty<Color> color = new SimpleObjectProperty<>(Color.CRIMSON);
	private final Popup popup;
	private final LuxColorPalette palette;

	public ColorBar() {
		setAlignment(Pos.CENTER_LEFT);
		getStyleClass().add("iconBar");
		palette = new LuxColorPalette(this);
		popup = new Popup();
		popup.getContent().add(palette);

		GeneratedIcon icon = new GeneratedIcon();
		JavaFX.size(icon, 32, 32);
		icon.setIcon(AwesomeIcon.EYEDROPPER);
		icon.getStyleClass().add("color-bar-icon");
		icon.setStyle("-icon-color: black");
		configureIcon(icon);
		icon.setOnMouseClicked(event -> {

			/*
			 * JavaFX makes it impossible to augment the root user agent stylesheet in a way
			 * that cascades to all new windows, of which Popup is one. This works around
			 * the problem by copying the stylesheets from the picker buttons root (the most
			 * likely place for the stylesheets).
			 */
			addIfNotAdded(popup.getScene().getRoot().getStylesheets(),
					ColorBar.this.getScene().getWindow().getScene().getRoot().getStylesheets());

			originalColor = getColor();
			palette.setColor(originalColor);
			Scene scene = getScene();
			Window window = scene.getWindow();
			popup.show(window);
			popup.setAutoHide(true);
			popup.setOnAutoHide(event2 -> updateHistory());
			Bounds buttonBounds = getBoundsInLocal();
			Point2D point = localToScreen(buttonBounds.getMinX(), buttonBounds.getMaxY());
			popup.setX(point.getX() - 9);
			popup.setY(point.getY() - 9);
		});
		getChildren().add(wrap(icon, bundle.getString("choose")));

		int i = 0;
		for (Color c : COLORS) {
			GeneratedIcon colIcon = new GeneratedIcon();
			colIcon.getStyleClass().add("color-bar-icon");
			colIcon.setStyle("-icon-opacity: 1");
			colIcon.setStyle("-icon-color: " + JavaFX.toHex(c));
			JavaFX.size(colIcon, 16, 16);
			colIcon.setOnMouseClicked(event -> {
				setColor(c);
			});
			configureIcon(colIcon);
			getChildren().add(wrap(colIcon, COLOR_NAMES[i++]));
		}

		colorProperty().addListener((e, o, n) -> icon.setStyle("-icon-color: " + JavaFX.toHex(n)));

		GeneratedIcon resetIcon = new GeneratedIcon();
		JavaFX.size(resetIcon, 32, 32);
		resetIcon.setIcon(AwesomeIcon.ERASER);
		resetIcon.getStyleClass().add("color-bar-icon");
		resetIcon.setStyle("-icon-color: black");
		configureIcon(resetIcon);
		resetIcon.setOnMouseClicked(event -> {
			setColor(Color.BLACK);
		});
		getChildren().add(wrap(resetIcon, bundle.getString("reset")));

	}

	protected Label wrap(Node icon, String text) {
		Label l = new Label();
		l.setGraphic(icon);
		Tooltip tooltip = new Tooltip(text);
		tooltip.setShowDelay(Duration.millis(200));
		l.setTooltip(tooltip);
		return l;
	}

	protected void configureIcon(GeneratedIcon colIcon) {
		colIcon.setOnMouseEntered((e) -> colIcon.setEffect(new Glow(0.9)));
		colIcon.setOnMouseExited((e) -> colIcon.setEffect(null));
	}

	/**
	 * Store the current color in the history palette.
	 */
	public void updateHistory() {
		palette.addToHistory(color.get());
	}

	public ObjectProperty<Color> colorProperty() {
		return color;
	}

	public void setColor(Color value) {
		color.set(value);
		palette.updateRandomColorSamples();
	}

	public Color getColor() {
		return color.get();
	}

	/**
	 * Hide the color picker popup.
	 */
	public void hidePopup() {
		popup.hide();
	}

	public void revertToOriginalColor() {
		setColor(originalColor);
	}

	public static List<String> addIfNotAdded(List<String> target, List<String> source) {
		for (String s : source) {
			if (!target.contains(s))
				target.add(s);
		}
		return target;
	}
}
