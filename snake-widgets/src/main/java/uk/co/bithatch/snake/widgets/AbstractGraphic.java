package uk.co.bithatch.snake.widgets;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.canvas.Canvas;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public abstract class AbstractGraphic extends Canvas {

	private static final StyleablePropertyFactory<AbstractGraphic> FACTORY = new StyleablePropertyFactory<>(
			Canvas.getClassCssMetaData());
	private static final CssMetaData<AbstractGraphic, Color> OUTLINE_COLOR = FACTORY
			.createColorCssMetaData("-snake-outline-color", s -> s.outlineColorProperty, Color.GRAY, true);

	public abstract void draw();

	protected static final PseudoClass PSEUDO_CLASS_SELECTED = PseudoClass.getPseudoClass("selected");

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return FACTORY.getCssMetaData();
	}

	private final StyleableProperty<Color> outlineColorProperty = new SimpleStyleableObjectProperty<>(OUTLINE_COLOR,
			this, "outlineColor");
	protected final ObjectProperty<Paint> ledColorProperty = new SimpleObjectProperty<>(this, "ledColor");
	protected final BooleanProperty selectedProperty = new SimpleBooleanProperty();

	public AbstractGraphic() {
		super();
	}

	public AbstractGraphic(double width, double height) {
		super(width, height);
		widthProperty().addListener((e, o, n) -> draw());
		heightProperty().addListener((e, o, n) -> draw());
		ledColorProperty.addListener((e, o, n) -> draw());
		selectedProperty.addListener((c, o, n) -> {
			pseudoClassStateChanged(PSEUDO_CLASS_SELECTED, isSelected());
			applyCss();
			draw();
		});
	}

	@Override
	public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
		return FACTORY.getCssMetaData();
	}

	public final Color getOutlineColor() {
		return outlineColorProperty.getValue();
	}

	@SuppressWarnings("unchecked")
	public ObservableValue<Color> outlineColorProperty() {
		return (ObservableValue<Color>) outlineColorProperty;
	}

	public final void setOutlineColor(Color outlineColor) {
		outlineColorProperty.setValue(outlineColor);
	}

	public final boolean isSelected() {
		return selectedProperty.getValue();
	}

	public BooleanProperty selectedProperty() {
		return selectedProperty;
	}

	public final void setSelected(boolean selected) {
		selectedProperty.setValue(selected);
	}

	public final Paint getLedColor() {
		return ledColorProperty.getValue();
	}

	public ObservableValue<Paint> ledColorProperty() {
		return (ObservableValue<Paint>) ledColorProperty;
	}

	public final void setLedColor(Paint ledColor) {
		ledColorProperty.setValue(ledColor);
	}

}