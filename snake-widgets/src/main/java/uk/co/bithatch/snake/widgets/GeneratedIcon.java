package uk.co.bithatch.snake.widgets;

import java.util.List;

import com.sshtools.icongenerator.AwesomeIcon;
import com.sshtools.icongenerator.IconBuilder;
import com.sshtools.icongenerator.IconBuilder.IconShape;
import com.sshtools.icongenerator.IconBuilder.TextContent;
import com.sshtools.icongenerator.javafx.JavaFXCanvasGenerator;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
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

public class GeneratedIcon extends Pane {

	private static final StyleablePropertyFactory<GeneratedIcon> FACTORY = new StyleablePropertyFactory<>(
			Pane.getClassCssMetaData());

	private static final CssMetaData<GeneratedIcon, Color> BORDER_COLOR = FACTORY
			.createColorCssMetaData("-icon-border-color", s -> s.borderColorProperty, null, false);

	private static final CssMetaData<GeneratedIcon, Number> BORDER_WIDTH = FACTORY
			.createSizeCssMetaData("-icon-border-width", s -> s.borderWidthProperty, -1, false);

	private static final CssMetaData<GeneratedIcon, Number> ICON_OPACITY = FACTORY
			.createSizeCssMetaData("-icon-opacity", s -> s.iconOpacityProperty, -1, false);

	private static final CssMetaData<GeneratedIcon, Number> FONT_SIZE = FACTORY.createSizeCssMetaData("-icon-font-size",
			s -> s.fontSizeProperty, -1, true);

	private static final CssMetaData<GeneratedIcon, Color> TEXT_COLOR = FACTORY
			.createColorCssMetaData("-icon-text-color", s -> s.textColorProperty, null, false);

	private static final CssMetaData<GeneratedIcon, Color> ICON_COLOR = FACTORY.createColorCssMetaData("-icon-color",
			s -> s.colorProperty, null, false);

	private static final CssMetaData<GeneratedIcon, Font> FONT = FACTORY.createFontCssMetaData("-icon-text-font",
			s -> s.fontProperty, null, false);

	private static final CssMetaData<GeneratedIcon, String> ICON_SHAPE = FACTORY.createStringCssMetaData("-icon-shape",
			s -> s.iconShapeProperty, null, false);

	private static final CssMetaData<GeneratedIcon, String> TEXT_CONTENT = FACTORY
			.createStringCssMetaData("-icon-text-content", s -> s.textContentProperty, null, false);

	private final StyleableProperty<Color> borderColorProperty = new SimpleStyleableObjectProperty<>(BORDER_COLOR, this,
			"borderColor");
	private final StyleableProperty<Font> fontProperty = new SimpleStyleableObjectProperty<>(FONT, this, "font");
	private final StyleableProperty<Number> borderWidthProperty = new SimpleStyleableDoubleProperty(BORDER_WIDTH, this,
			"borderWidth");
	private final StyleableProperty<Number> iconOpacityProperty = new SimpleStyleableDoubleProperty(ICON_OPACITY, this,
			"iconOpacity");
	private final StyleableProperty<Number> fontSizeProperty = new SimpleStyleableDoubleProperty(FONT_SIZE, this,
			"fontSize");
	private final StyleableProperty<Color> textColorProperty = new SimpleStyleableObjectProperty<>(TEXT_COLOR, this,
			"textColor");
	private final StyleableProperty<Color> colorProperty = new SimpleStyleableObjectProperty<>(ICON_COLOR, this,
			"color");
	private final StyleableProperty<String> iconShapeProperty = new SimpleStyleableObjectProperty<>(ICON_SHAPE, this,
			"iconShape");
	private final StyleableProperty<String> textContentProperty = new SimpleStyleableObjectProperty<>(TEXT_CONTENT,
			this, "textContent");
	private final StringProperty textProperty = new SimpleStringProperty();
	private final ObjectProperty<AwesomeIcon> iconProperty = new SimpleObjectProperty<AwesomeIcon>(this, "icon", null);

	private IconBuilder ib;

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return FACTORY.getCssMetaData();
	}

	public GeneratedIcon() {
		ib = new IconBuilder();
		
		/* NOTE: Hack to make it work inside SceneBuilder */
		ib.generator(Canvas.class, new JavaFXCanvasGenerator());

		widthProperty().addListener((c, o, n) -> {
			ib.width((float) prefWidth(0));
			rebuild();
		});
		heightProperty().addListener((c, o, n) -> {
			ib.height((float) prefHeight(0));
			rebuild();
		});
		borderColorProperty().addListener((c, o, n) -> {
			if (n == null)
				ib.borderColor(IconBuilder.AUTO_COLOR);
			else
				ib.borderColor(JavaFX.encode(n));
			rebuild();
		});
		fontProperty().addListener((c, o, n) -> {
			resetFont();
			rebuild();
		});
		borderWidthProperty().addListener((c, o, n) -> {
			if (getBorderWidth() == -1)
				ib.border(0);
			else
				ib.border((float) getBorderWidth());
			rebuild();
		});
		iconOpacityProperty().addListener((c, o, n) -> {
			int opac = (int) (n.doubleValue() * 255.0);
			ib.backgroundOpacity(opac);
			rebuild();
		});
		textColorProperty().addListener((c, o, n) -> {
			if (n == null)
				ib.textColor(IconBuilder.AUTO_TEXT_COLOR);
			else
				ib.textColor(JavaFX.encode(getTextColor()));
			rebuild();
		});
		colorProperty().addListener((c, o, n) -> {
			if (n == null) {
				ib.color(IconBuilder.AUTO_COLOR);
			} else {
				ib.color(JavaFX.encode(getColor()));
			}
			rebuild();
		});
		iconShapeProperty().addListener((c, o, n) -> {
			if (n == null)
				ib.shape(IconShape.AUTOMATIC);
			else
				ib.shape(getIconShape());
			rebuild();
		});
		textContentProperty().addListener((c, o, n) -> {
			ib.textContent(n == null ? null : TextContent.valueOf(n));
			rebuild();
		});
		textProperty().addListener((c, o, n) -> {
			ib.text(n);
			rebuild();
		});
		iconProperty().addListener((c, o, n) -> {
			ib.icon(n);
			rebuild();
		});
		fontSizeProperty().addListener((c, o, n) -> {
			resetFont();
			rebuild();
		});

		setupIcon();
	}

	void setupIcon() {
		ib.width((float) prefWidth(0));
		ib.height((float) prefHeight(0));
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
			ib.border((float) getBorderWidth());
		if (getBorderColor() != null)
			ib.borderColor(JavaFX.encode(getBorderColor()));
		if (getIconOpacity() != -1) {
			int opac = (int) (getIconOpacity() * 255.0);
			ib.backgroundOpacity(opac);
		}
		resetFont();
		if (getIcon() != null)
			ib.icon(getIcon());
		ib.bold(true);
		rebuild();
	}

	protected void resetFont() {
		int fs = getFontSize();
		if (getFont() != null) {
			ib.fontName(getFont().getFamily());
			if (fs < 1)
				fs = (int) (getPrefWidth() * 0.5f);
		}
		if (fs > 0) {
			ib.fontSize(fs);
		} else
			ib.fontSize(0);
	}

	protected void rebuild() {
		Canvas build = ib.build(Canvas.class);
		if (getChildren().isEmpty())
			getChildren().add(build);
		else
			getChildren().set(0, build);
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
		return colorProperty.getValue();
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

	public final int getFontSize() {
		return fontSizeProperty.getValue().intValue();
	}

	public final double getIconOpacity() {
		return iconOpacityProperty.getValue().doubleValue();
	}

	@SuppressWarnings("unchecked")
	public ObservableValue<Font> fontProperty() {
		return (ObservableValue<Font>) fontProperty;
	}

	@SuppressWarnings("unchecked")
	public ObservableValue<Number> fontSizeProperty() {
		return (ObservableValue<Number>) fontSizeProperty;
	}

	public final void setFont(Font font) {
		fontProperty.setValue(font);
	}

	public final void setFontSize(int fontSize) {
		fontSizeProperty.setValue(fontSize);
	}

	public final void setIconOpacity(double iconOpacity) {
		iconOpacityProperty.setValue(iconOpacity);
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

	public final AwesomeIcon getIcon() {
		return iconProperty.getValue();
	}

	public ObservableValue<AwesomeIcon> iconProperty() {
		return (ObservableValue<AwesomeIcon>) iconProperty;
	}

	@SuppressWarnings("unchecked")
	public ObservableValue<Number> iconOpacityProperty() {
		return (ObservableValue<Number>) iconOpacityProperty;
	}

	public final void setIcon(AwesomeIcon icon) {
		iconProperty.setValue(icon);
	}
}
