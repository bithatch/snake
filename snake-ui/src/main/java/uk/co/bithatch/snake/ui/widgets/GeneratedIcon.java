package uk.co.bithatch.snake.ui.widgets;

import java.util.List;

import com.sshtools.icongenerator.IconBuilder;
import com.sshtools.icongenerator.IconBuilder.IconShape;
import com.sshtools.icongenerator.IconBuilder.TextContent;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import uk.co.bithatch.snake.ui.util.JavaFX;

public class GeneratedIcon extends Pane {

	private static final StyleablePropertyFactory<GeneratedIcon> FACTORY = new StyleablePropertyFactory<>(
			Pane.getClassCssMetaData());

	private static final CssMetaData<GeneratedIcon, Color> BORDER_COLOR = FACTORY
			.createColorCssMetaData("-icon-border-color", s -> s.borderColorProperty, null, true);

	private static final CssMetaData<GeneratedIcon, Number> BORDER_WIDTH = FACTORY
			.createSizeCssMetaData("-icon-border-width", s -> s.borderWidthProperty, -1, true);
	
	private static final CssMetaData<GeneratedIcon, Number> OPACITY = FACTORY.createSizeCssMetaData("-icon-opacity",
			s -> s.opacityProperty, -1, true);

	private static final CssMetaData<GeneratedIcon, Color> TEXT_COLOR = FACTORY
			.createColorCssMetaData("-icon-text-color", s -> s.textColorProperty, null, true);

	private static final CssMetaData<GeneratedIcon, Color> ICON_COLOR = FACTORY.createColorCssMetaData("-icon-color",
			s -> s.colorProperty, null, true);

	private static final CssMetaData<GeneratedIcon, Font> FONT = FACTORY.createFontCssMetaData("-icon-text-font",
			s -> s.fontProperty, null, true);

	private static final CssMetaData<GeneratedIcon, String> SHAPE = FACTORY.createStringCssMetaData("-icon-shape",
			s -> s.iconShapeProperty, null, true);

	private static final CssMetaData<GeneratedIcon, String> TEXT_CONTENT = FACTORY
			.createStringCssMetaData("-icon-text-content", s -> s.textContentProperty, null, true);

	private final StyleableProperty<Color> borderColorProperty = new SimpleStyleableObjectProperty<>(BORDER_COLOR, this,
			"borderColor");
	private final StyleableProperty<Font> fontProperty = new SimpleStyleableObjectProperty<>(FONT, this, "font");
	private final StyleableProperty<Number> borderWidthProperty = new SimpleStyleableDoubleProperty(BORDER_WIDTH, this,
			"borderWidth");
	private final StyleableProperty<Number> opacityProperty = new SimpleStyleableDoubleProperty(OPACITY, this,
			"opacity");
	private final StyleableProperty<Color> textColorProperty = new SimpleStyleableObjectProperty<>(TEXT_COLOR, this,
			"textColor");
	private final StyleableProperty<Color> colorProperty = new SimpleStyleableObjectProperty<>(ICON_COLOR, this,
			"color");
	private final StyleableProperty<String> iconShapeProperty = new SimpleStyleableObjectProperty<>(SHAPE, this,
			"shape");
	private final StyleableProperty<String> textContentProperty = new SimpleStyleableObjectProperty<>(TEXT_CONTENT,
			this, "textContent");
	private final StringProperty textProperty = new SimpleStringProperty();

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return FACTORY.getCssMetaData();
	}

	public GeneratedIcon() {
		widthProperty().addListener((c, o, n) -> buildIcon());
		heightProperty().addListener((c, o, n) -> buildIcon());
		borderColorProperty().addListener((c, o, n) -> buildIcon());
		fontProperty().addListener((c, o, n) -> buildIcon());
		borderWidthProperty().addListener((c, o, n) -> buildIcon());
		opacityProperty().addListener((c, o, n) -> buildIcon());
		textColorProperty().addListener((c, o, n) -> buildIcon());
		colorProperty().addListener((c, o, n) -> buildIcon());
		iconShapeProperty().addListener((c, o, n) -> buildIcon());
		textContentProperty().addListener((c, o, n) -> buildIcon());
		textProperty().addListener((c, o, n) -> buildIcon());
		buildIcon();
	}

	void buildIcon() {
		IconBuilder ib = new IconBuilder();
		ib.width((float) getWidth());
		ib.height((float) getHeight());
		ib.text(getText());
		if (getTextContent() != null)
			ib.textContent(getTextContent());
		if (getColor() != null)
			ib.color(JavaFX.encode(getColor()));
		if (getTextColor() != null)
			ib.textColor(JavaFX.encode(getTextColor()));
		if (getIconShape() != null)
			ib.shape(getIconShape());
		if (getBorderWidth() != -1)
			ib.border((float)getBorderWidth());
		if (getBorderColor() != null)
			ib.borderColor(JavaFX.encode(getBorderColor()));
		if (getOpacity() != -1)
			ib.backgroundOpacity(0);
		if(getFont() != null) {
			ib.fontName(getFont().getFamily());
			ib.fontSize((int)Math.round(getFont().getSize()));
		}
		ib.bold(true);
		getChildren().clear();
		Canvas build = ib.build(Canvas.class);
		getChildren().add(build);
	}

	@Override
	public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
		return FACTORY.getCssMetaData();
	}

	public final Color getBorderColor() {
		return borderColorProperty.getValue();
	}

	@SuppressWarnings("unchecked")
	public ObservableValue<Color> borderColorProperty() {
		return (ObservableValue<Color>) borderColorProperty;
	}

	public final void setBorderColor(Color borderColor) {
		borderColorProperty.setValue(borderColor);
	}

	public final double getBorderWidth() {
		return borderWidthProperty.getValue().doubleValue();
	}

	@SuppressWarnings("unchecked")
	public ObservableValue<Number> borderWidthProperty() {
		return (ObservableValue<Number>) borderWidthProperty;
	}

	public final void setBorderWidth(double borderWidth) {
		borderWidthProperty.setValue(borderWidth);
	}

	public final Color getTextColor() {
		return textColorProperty.getValue();
	}

	@SuppressWarnings("unchecked")
	public ObservableValue<Color> textColorProperty() {
		return (ObservableValue<Color>) textColorProperty;
	}

	public final void setTextColor(Color textColor) {
		textColorProperty.setValue(textColor);
	}

	public final Color getColor() {
		return textColorProperty.getValue();
	}

	@SuppressWarnings("unchecked")
	public ObservableValue<Color> colorProperty() {
		return (ObservableValue<Color>) colorProperty;
	}

	public final void setColor(Color color) {
		colorProperty.setValue(color);
	}

	public final Font getFont() {
		return fontProperty.getValue();
	}

	@SuppressWarnings("unchecked")
	public ObservableValue<Font> fontProperty() {
		return (ObservableValue<Font>) fontProperty;
	}

	public final void setFont(Font font) {
		fontProperty.setValue(font);
	}

	public final IconShape getIconShape() {
		return iconShapeProperty.getValue() == null ? null : IconShape.valueOf(iconShapeProperty.getValue());
	}

	@SuppressWarnings("unchecked")
	public ObservableValue<String> iconShapeProperty() {
		return (ObservableValue<String>) iconShapeProperty;
	}

	public final void setIconShape(IconShape shape) {
		iconShapeProperty.setValue(shape == null ? null : shape.name());
	}

	public final TextContent getTextContent() {
		return textContentProperty.getValue() == null ? null : TextContent.valueOf(textContentProperty.getValue());
	}

	@SuppressWarnings("unchecked")
	public ObservableValue<String> textContentProperty() {
		return (ObservableValue<String>) textContentProperty;
	}

	public final void setTextContent(TextContent textContent) {
		textContentProperty.setValue(textContent == null ? null : textContent.name());
	}

	public final String getText() {
		return textProperty.getValue();
	}

	public ObservableValue<String> textProperty() {
		return (ObservableValue<String>) textProperty;
	}

	public final void setText(String text) {
		textProperty.setValue(text);
	}
}
