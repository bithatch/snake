package uk.co.bithatch.snake.ui.designer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.css.converter.SizeConverter;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import uk.co.bithatch.snake.lib.BrandingImage;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Device.Listener;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.Region.Name;
import uk.co.bithatch.snake.lib.layouts.Area;
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.DeviceView;
import uk.co.bithatch.snake.lib.layouts.IO;
import uk.co.bithatch.snake.lib.layouts.Key;
import uk.co.bithatch.snake.lib.layouts.LED;
import uk.co.bithatch.snake.lib.layouts.MatrixCell;
import uk.co.bithatch.snake.lib.layouts.MatrixIO;
import uk.co.bithatch.snake.lib.layouts.ViewPosition;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.ListMultipleSelectionModel;
import uk.co.bithatch.snake.ui.util.BasicList;
import uk.co.bithatch.snake.ui.util.JavaFX;
import uk.co.bithatch.snake.ui.util.ListWrapper;
import uk.co.bithatch.snake.ui.util.Strings;
import uk.co.bithatch.snake.ui.widgets.SelectableArea;

public class LayoutEditor extends SelectableArea implements ViewerView, Listener {

	public interface LabelFactory {
		Node createLabel(IO element);
	}

	class DeviceCanvas extends Canvas {

		DeviceCanvas() {
			super(512, 512);
			draw();
			widthProperty().addListener((e, o, n) -> draw());
			heightProperty().addListener((e, o, n) -> draw());
		}

		protected Point2D calcImageOffset() {

			double canvasWidth = boundsInLocalProperty().get().getWidth();
			double canvasHeight = boundsInLocalProperty().get().getHeight();

			Point2D imgSize = calcImageSize();

			double imgX = (canvasWidth - imgSize.getX()) / 2;
			double imgY = (canvasHeight - imgSize.getY()) / 2;

			return new Point2D(imgX, imgY);
		}

		protected Point2D calcImageSize() {

			double canvasWidth = boundsInLocalProperty().get().getWidth();
			double canvasHeight = boundsInLocalProperty().get().getHeight();

			if (img == null)
				return new Point2D(canvasWidth, canvasHeight);

			/*
			 * Get how much we would have to scale the width by for it cover the entire area
			 */
			double widthScale = canvasWidth / img.getWidth();

			/*
			 * Get how much we would have to scale the height by for it cover the entire
			 * area
			 */
			double heightScale = canvasHeight / img.getHeight();

			/*
			 * Scale the image so it fits the height (preserving ratio). If the width then
			 * exceeds available space, then scale the height back until it fits
			 */
			double imgHeight = img.getHeight() * heightScale;
			double imgWidth = img.getWidth() * heightScale;
			if (imgWidth > canvasWidth) {
				imgHeight = img.getHeight() * widthScale;
				imgWidth = img.getWidth() * widthScale;
			}

			return new Point2D(imgWidth * view.getImageScale(), imgHeight * view.getImageScale());

		}

		protected void draw() {
			GraphicsContext ctx = getGraphicsContext2D();

			double canvasWidth = boundsInLocalProperty().get().getWidth();
			double canvasHeight = boundsInLocalProperty().get().getHeight();
			ctx.clearRect(0, 0, canvasWidth, canvasHeight);

			Point2D imgSize = calcImageSize();
			Point2D imgOff = calcImageOffset();

			if (!isLayoutReadOnly()) {
				ctx.setGlobalAlpha(0.2f);
				double gw = Math.max(1, gridSizeSlider.valueProperty().get() * imgSize.getX());
				double gh = Math.max(1, gridSizeSlider.valueProperty().get() * imgSize.getY());
				ctx.setStroke(getLineColor());
				ctx.beginPath();
				ctx.setLineWidth(0.5);
				for (double y = imgOff.getY() % gh; y < canvasHeight; y += gh) {
					ctx.moveTo(0, y);
					ctx.lineTo(canvasWidth, y);
				}
				for (double x = imgOff.getX() % gw; x < canvasWidth; x += gw) {
					ctx.moveTo(x, 0);
					ctx.lineTo(x, canvasHeight);
				}

				ctx.stroke();
				ctx.closePath();
				ctx.setGlobalAlpha(1);
			}
			ctx.setGlobalAlpha(view.getImageOpacity());
			if (view.isDesaturateImage()) {
				ColorAdjust adjust = new ColorAdjust();
				adjust.setSaturation(-1);
				ctx.setEffect(adjust);
			}
			ctx.drawImage(img, imgOff.getX(), imgOff.getY(), imgSize.getX(), imgSize.getY());
			ctx.setGlobalAlpha(1);
			if (view.isDesaturateImage()) {
				ctx.setEffect(null);
			}

			List<ElementView> left = new ArrayList<>();
			List<ElementView> top = new ArrayList<>();
			List<ElementView> bottom = new ArrayList<>();
			List<ElementView> right = new ArrayList<>();
			separateElements(left, top, bottom, right);

			double totalHeight = getTotalHeight(left);
			double y = ((canvasHeight - (totalHeight)) / 2f);

			/* Left */
			for (ElementView elementView : left) {
				IO element = elementView.getElement();
				double lineWidth = elementView.getLabel().getBoundsInLocal().getWidth();
				double rowHeight = elementView.getLabel().getBoundsInLocal().getHeight();
				ctx.setFont(font);

				double elementX = elementXToLocalX(element.getX());
				double elementY = elementYToLocalY(element.getY());
				double xToElement = elementX - lineWidth - (getGraphicTextGap() * 2);

				ctx.setStroke(getLineColor());
				ctx.beginPath();
				ctx.setLineWidth(getLineWidth());
				ctx.moveTo(lineWidth + getGraphicTextGap(), y + (rowHeight / 2f));
				ctx.lineTo(lineWidth + getGraphicTextGap() + Math.max(1, (xToElement * 0.1)), y + (rowHeight / 2f));
				ctx.lineTo(elementX - Math.max(1, (xToElement * 0.1)) - getGraphicTextGap(), elementY);
				ctx.lineTo(elementX - getGraphicTextGap(), elementY);
				ctx.stroke();
				ctx.closePath();

				y += rowHeight;

			}

			/* Right */
			totalHeight = getTotalHeight(right);
			y = ((canvasHeight - (totalHeight)) / 2f);
			for (ElementView elementView : right) {
				IO element = elementView.getElement();

				double lineWidth = elementView.getLabel().getBoundsInLocal().getWidth();
				double rowHeight = elementView.getLabel().getBoundsInLocal().getHeight();

				double elementX = elementXToLocalX(element.getX());
				double elementY = elementYToLocalY(element.getY());
				double xToElement = canvasWidth - elementX - lineWidth - (getGraphicTextGap() * 2);

				ctx.setStroke(getLineColor());
				ctx.beginPath();
				ctx.setLineWidth(getLineWidth());
				ctx.moveTo(canvasWidth - lineWidth - getGraphicTextGap(), y + (rowHeight / 2f));
				ctx.lineTo(canvasWidth - lineWidth - Math.max(1, (xToElement * 0.1)) - getGraphicTextGap(),
						y + (rowHeight / 2f));
				ctx.lineTo(elementX + Math.max(1, (xToElement * 0.1)) + getGraphicTextGap(), elementY);
				ctx.lineTo(elementX + getGraphicTextGap(), elementY);
				ctx.stroke();
				ctx.closePath();

				y += rowHeight;
			}

			/* Top */
			double totalTextWidth = getTotalWidth(top);

			double x = ((canvasWidth - totalTextWidth) / 2f);
			for (ElementView elementView : top) {
				IO element = elementView.getElement();
				double lineWidth = elementView.getLabel().getBoundsInLocal().getWidth();
				double rowHeight = elementView.getLabel().getBoundsInLocal().getHeight();

				double elementX = elementXToLocalX(element.getX());
				double elementY = elementYToLocalY(element.getY());
				double yToElement = canvasHeight - elementY - rowHeight - (getGraphicTextGap() * 2);

				ctx.setStroke(getLineColor());
				ctx.beginPath();
				ctx.setLineWidth(getLineWidth());
				ctx.moveTo(x + (lineWidth / 2f), rowHeight + getGraphicTextGap());
				ctx.lineTo(x + (lineWidth / 2f), rowHeight + Math.max(1, (yToElement * 0.1)) + getGraphicTextGap());
				ctx.lineTo(elementX, elementY - Math.max(1, (yToElement * 0.1)) - getGraphicTextGap());
				ctx.lineTo(elementX, elementY - getGraphicTextGap());
				ctx.stroke();
				ctx.closePath();

				x += lineWidth + getGraphicTextGap();
			}

			/* Bottom */
			totalTextWidth = getTotalWidth(bottom);

			x = ((canvasWidth - totalTextWidth) / 2f);
			for (ElementView elementView : bottom) {
				IO element = elementView.getElement();

				double lineWidth = elementView.getLabel().getBoundsInLocal().getWidth();
				double rowHeight = elementView.getLabel().getBoundsInLocal().getHeight();

				double elementX = elementXToLocalX(element.getX());
				double elementY = elementYToLocalY(element.getY());
				double yToElement = canvasHeight - elementY - rowHeight - (getGraphicTextGap() * 2);

				ctx.setStroke(getLineColor());
				ctx.beginPath();
				ctx.setLineWidth(getLineWidth());
				ctx.moveTo(elementX, elementY + getGraphicTextGap());
				ctx.lineTo(elementX, elementY + getGraphicTextGap() + Math.max(1, (yToElement * 0.1)));
				ctx.lineTo(x + (lineWidth / 2f),
						canvasHeight - rowHeight - Math.max(1, (yToElement * 0.1)) - getGraphicTextGap());
				ctx.lineTo(x + (lineWidth / 2f), canvasHeight - rowHeight - getGraphicTextGap());
				ctx.stroke();
				ctx.closePath();

				x += lineWidth + getGraphicTextGap();
			}
		}

		protected double elementXToLocalX(double x) {
			Point2D imgSize = calcImageSize();
			Point2D imgOff = calcImageOffset();
			return (x * imgSize.getX()) + imgOff.getX();
		}

		protected double elementYToLocalY(double y) {
			Point2D imgSize = calcImageSize();
			Point2D imgOff = calcImageOffset();
			return (y * imgSize.getY()) + imgOff.getY();
		}

		protected void updateElement(ElementView element, double toolX, double toolY) {
			Point2D pc = getElementPosition(toolX, toolY);
			element.getElement().setX((float) pc.getX());
			element.getElement().setY((float) pc.getY());
		}

		protected Point2D getElementPosition(double toolX, double toolY) {
			float pcX, pcY;
			if (img != null) {
				Point2D imgSize = calcImageSize();
				Point2D imgOff = calcImageOffset();

				double scale = imgSize.getX() / img.getWidth();
				double pixelX = (toolX - imgOff.getX()) * scale;
				double pixelY = (toolY - imgOff.getY()) * scale;

				pcX = (float) ((pixelX / (imgSize.getX() * scale)));
				pcY = (float) ((pixelY / (imgSize.getY() * scale)));
			} else {
				pcX = (float) ((toolX / (boundsInLocalProperty().get().getWidth())));
				pcY = (float) ((toolY / (boundsInLocalProperty().get().getHeight())));
			}

			if (snapToGridCheckBox.selectedProperty().get()) {
				pcX = (float) (pcX - (pcX % gridSizeSlider.getValue()) + (gridSizeSlider.getValue() / 2.0));
				pcY = (float) (pcY - (pcY % gridSizeSlider.getValue()) + (gridSizeSlider.getValue() / 2.0));
			}
			return new Point2D(pcX, pcY);
		}
	}

	private static class StyleableProperties {
		private static final CssMetaData<LayoutEditor, Number> GRAPHIC_TEXT_GAP = new CssMetaData<LayoutEditor, Number>(
				"-fx-graphic-text-gap", SizeConverter.getInstance(), 4.0) {

			@SuppressWarnings("unchecked")
			@Override
			public StyleableProperty<Number> getStyleableProperty(LayoutEditor n) {
				return (StyleableProperty<Number>) n.graphicTextGapProperty();
			}

			@Override
			public boolean isSettable(LayoutEditor n) {
				return n.graphicTextGap == null || !n.graphicTextGap.isBound();
			}
		};
		private static final CssMetaData<LayoutEditor, Number> LINE_WIDTH = new CssMetaData<LayoutEditor, Number>(
				"-snake-line-width", SizeConverter.getInstance(), DEFAULT_LINE_WIDTH) {

			@SuppressWarnings("unchecked")
			@Override
			public StyleableProperty<Number> getStyleableProperty(LayoutEditor n) {
				return (StyleableProperty<Number>) n.lineWidthProperty();
			}

			@Override
			public boolean isSettable(LayoutEditor n) {
				return n.graphicTextGap == null || !n.graphicTextGap.isBound();
			}
		};
	}

	final static Insets DEFAULT_INSETS = new Insets(20);
	private static final double DEFAULT_GRAPHICS_TEXT_GAP = 8;

	private static final double DEFAULT_LINE_WIDTH = 1;
	private static final StyleablePropertyFactory<LayoutEditor> FACTORY = new StyleablePropertyFactory<>(
			SelectableArea.getClassCssMetaData());

	private static final CssMetaData<LayoutEditor, Color> LINE_COLOR = FACTORY
			.createColorCssMetaData("-snake-line-color", s -> s.lineColorProperty, Color.GRAY, true);

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return FACTORY.getCssMetaData();
	}

	private final BooleanProperty showElementGraphics = new SimpleBooleanProperty(this, "showElementGraphics", true);
	private final ObjectProperty<ComponentType> componentType = new SimpleObjectProperty<>(this, "componentType");
	private final ObjectProperty<LabelFactory> labelFactory = new SimpleObjectProperty<>(this, "labelFactory");
	private final ObjectProperty<List<ComponentType>> enabledTypes = new SimpleObjectProperty<>(this, "enabledTypes");
	private final Slider gridSizeSlider = new Slider(0.01, 0.1, 0.025);
	private final ObjectProperty<MultipleSelectionModel<IO>> keySelectionModel = new SimpleObjectProperty<MultipleSelectionModel<IO>>(
			this, "keySelectionModel");
	private final StyleableProperty<Color> lineColorProperty = new SimpleStyleableObjectProperty<>(LINE_COLOR, this,
			"lineColor");
	private final CheckBox snapToGridCheckBox = new CheckBox();

	private Image img;
	private ListWrapper<ElementView, IO> items;
	private DoubleProperty lineWidth;
	private String loadedImage;
	private Viewer viewer;
	private Font font;
	private DoubleProperty graphicTextGap;

	private App context;

	DeviceCanvas canvas;
	Device device;
	ObjectProperty<ObservableList<ElementView>> elements = new SimpleObjectProperty<>(this, "elements");
	List<Node> componentTypes = new ArrayList<>();
	DeviceView view;
	Pane pane;

	public LayoutEditor(App context) {
		this.context = context;
		font = Font.getDefault();
		enabledTypes.set(new BasicList<>());
		enabledTypes.addListener((c, o, n) -> {
			List<ComponentType> list = enabledTypes.get();
			if (!list.contains(componentType.get())) {
				if (list.isEmpty())
					componentType.set(null);
				else
					componentType.set(list.get(0));
			} else if (componentType.get() == null && !list.isEmpty())
				componentType.set(list.get(0));
			if (pane != null)
				rebuildComponentTypesPanel();
		});
		elements.set(new BasicList<ElementView>());
		elements.get().addListener(new ListChangeListener<>() {
			@Override
			public void onChanged(Change<? extends ElementView> c) {
				rebuildComponentTypesPanel();
			}
		});
		items = new ListWrapper<ElementView, IO>(elements.get()) {
			@Override
			protected IO doConvertToWrapped(ElementView in) {
				return in.getElement();
			}
		};
		setKeySelectionModel(new ListMultipleSelectionModel<>(items));

		setFocusTraversable(true);
		setOnKeyReleased((e) -> {
			if (e.isControlDown() && e.getCode() == KeyCode.A) {
				getKeySelectionModel().selectAll();
				e.consume();
			}
			if (e.getCode() == KeyCode.DELETE) {
				viewer.removeSelectedElements();
				e.consume();
			}
		});
		setOnKeyPressed((e) -> {
			if (e.getCode() == KeyCode.UP) {
				moveSelection(0, -(e.isShiftDown() ? 10 : 1));
				e.consume();
			} else if (e.getCode() == KeyCode.DOWN) {
				moveSelection(0, e.isShiftDown() ? 10 : 1);
				e.consume();
			} else if (e.getCode() == KeyCode.LEFT) {
				moveSelection(-(e.isShiftDown() ? 10 : 1), 0);
				e.consume();
			} else if (e.getCode() == KeyCode.RIGHT) {
				moveSelection(e.isShiftDown() ? 10 : 1, 0);
				e.consume();
			}
		});
		showElementGraphics.addListener((e, o, n) -> {
			rebuildElements();
		});
		snapToGridCheckBox.selectedProperty().addListener((e, o, n) -> {
			if (n)
				resnap();
		});
		gridSizeSlider.valueProperty().addListener((e, o, n) -> {
			if (snapToGridCheckBox.selectedProperty().get())
				resnap();
		});
		snapToGridCheckBox.textProperty().set(TabbedViewer.bundle.getString("snapToGrid"));

		selectableArea().addListener((e, o, n) -> {
			if (o == null && n != null) {
				clearSelection();
			}
			if (n != null) {
				for (ElementView b : elements.get()) {
					if (n.intersects(b.getElementTool().getBoundsInParent())) {
						addToSelection(b.getElement());
					}
				}
			}
		});

		keySelectionModel().get().getSelectedItems().addListener(new ListChangeListener<IO>() {
			@Override
			public void onChanged(Change<? extends IO> c) {
				while (c.next()) {
					if (c.wasAdded()) {
						for (IO io : c.getAddedSubList()) {
							ElementView ev = forElement(io);
							ev.getElementTool().setSelected(true);
						}
					}
					if (c.wasRemoved()) {
						for (IO io : c.getRemoved()) {
							ElementView ev = forElement(io);
							if (ev != null)
								ev.getElementTool().setSelected(false);
						}
					}
				}
			}
		});
	}

	public BooleanProperty showElementGraphics() {
		return showElementGraphics;
	}

	public void setShowElementGraphics(boolean showElementGraphics) {
		this.showElementGraphics.set(showElementGraphics);
	}

	public boolean isShowElementGraphics() {
		return showElementGraphics.get();
	}

	public void setEnabledTypes(List<ComponentType> enabledTypes) {
		this.enabledTypes.set(enabledTypes);
	}

	@Override
	public void changed(Device device, Region region) {
	}

	@Override
	public void close() throws Exception {
		device.removeListener(this);
	}

	public ObjectProperty<ComponentType> componentType() {
		return componentType;
	}

	public ComponentType getComponentType() {
		return componentType.get();
	}

	public ObjectProperty<LabelFactory> labelFactory() {
		return labelFactory;
	}

	public LabelFactory getLabelFactory() {
		return labelFactory.get();
	}

	@Override
	public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
		return FACTORY.getCssMetaData();
	}

	@Override
	public List<IO> getElements() {
		return items;
	}

	public List<ComponentType> getEnabledTypes() {
		return enabledTypes.get();
	}

	public ObjectProperty<List<ComponentType>> enabledTypes() {
		return enabledTypes;
	}

	public final double getGraphicTextGap() {
		return graphicTextGap == null ? DEFAULT_GRAPHICS_TEXT_GAP : graphicTextGap.getValue();
	}

	public final MultipleSelectionModel<IO> getKeySelectionModel() {
		return keySelectionModel == null ? null : keySelectionModel.get();
	}

	public final Color getLineColor() {
		return lineColorProperty.getValue();
	}

	public final double getLineWidth() {
		return lineWidth == null ? DEFAULT_LINE_WIDTH : lineWidth.getValue();
	}

	@Override
	public Node getRoot() {
		return this;
	}

	public final DoubleProperty graphicTextGapProperty() {
		if (graphicTextGap == null) {
			graphicTextGap = new StyleableDoubleProperty(4) {

				@Override
				public Object getBean() {
					return LayoutEditor.this;
				}

				@Override
				public CssMetaData<LayoutEditor, Number> getCssMetaData() {
					return StyleableProperties.GRAPHIC_TEXT_GAP;
				}

				@Override
				public String getName() {
					return "graphicTextGap";
				}
			};
		}
		return graphicTextGap;
	}

	public final ObjectProperty<MultipleSelectionModel<IO>> keySelectionModel() {
		return keySelectionModel;
	}

	@SuppressWarnings("unchecked")
	public ObservableValue<Color> lineColorProperty() {
		return (ObservableValue<Color>) lineColorProperty;
	}

	public final DoubleProperty lineWidthProperty() {
		if (lineWidth == null) {
			lineWidth = new StyleableDoubleProperty(DEFAULT_LINE_WIDTH) {

				@Override
				public Object getBean() {
					return LayoutEditor.this;
				}

				@Override
				public CssMetaData<LayoutEditor, Number> getCssMetaData() {
					return StyleableProperties.LINE_WIDTH;
				}

				@Override
				public String getName() {
					return "lineWidth";
				}
			};
		}
		return lineWidth;
	}

	@Override
	public void open(Device device, DeviceView view, Viewer viewer) {
		this.view = view;
		this.device = device;
		this.viewer = viewer;

		setEnabledTypes(viewer.getEnabledTypes());
		gridSizeSlider.visibleProperty().set(!viewer.isReadOnly());
		snapToGridCheckBox.visibleProperty().set(!viewer.isReadOnly());
		JavaFX.bindManagedToVisible(snapToGridCheckBox);
		JavaFX.bindManagedToVisible(gridSizeSlider);
		snapToGridCheckBox.visibleProperty().bind(Bindings.not(viewer.readOnly()));
		gridSizeSlider.visibleProperty().bind(Bindings.not(viewer.readOnly()));
		setSelectable(viewer.isSelectableElements());
		selectableProperty().bind(viewer.selectableElements());
		viewer.readOnly().addListener((e) -> {
			rebuildElements();
		});

		reloadImage();

		canvas = new DeviceCanvas();
		canvas.setPickOnBounds(false);
		canvas.setMouseTransparent(true);

		setDefaultClickHandler((e) -> {
			requestFocus();
			if (isLayoutSelectableElements())
				clearSelection();
		});

		pane = new Pane();
		pane.getStyleClass().add("layout");
		pane.setPickOnBounds(false);
		/* pane.getStyleClass().add("layout"); */
		setContent(pane);

		pane.getChildren().add(canvas);
		pane.getChildren().add(snapToGridCheckBox);
		pane.getChildren().add(gridSizeSlider);

		widthProperty().addListener((e, o, n) -> layoutDiagram());
		heightProperty().addListener((e, o, n) -> layoutDiagram());

		view.addListener(new DeviceView.Listener() {

			@Override
			public void elementRemoved(DeviceView view, IO element) {
				if (Platform.isFxApplicationThread()) {
					ElementView ev = forElement(element);
					if (ev != null) {
						int indexOf = elements.get().indexOf(ev);
						getKeySelectionModel().clearSelection(indexOf);
						elements.get().remove(ev);
						JavaFX.fadeHide(ev.getElementTool(), 0.25f, (e) -> {
							pane.getChildren().remove(ev.getElementTool());
							layoutDiagram();
						});
						if (ev.getLabel() != null) {
							JavaFX.fadeHide(ev.getLabel(), 0.25f, (e) -> {
								pane.getChildren().remove(ev.getLabel());
								layoutDiagram();
							});
						}
					}
				} else
					Platform.runLater(() -> elementRemoved(view, element));
			}

			@Override
			public void elementAdded(DeviceView view, IO element) {
				elementChanged(view, element);
			}

			@Override
			public void elementChanged(DeviceView view, IO element) {
				if (Platform.isFxApplicationThread()) {
					retextLabels();
					layoutLabels();
				} else
					Platform.runLater(() -> elementChanged(view, element));
			}

			@Override
			public void viewChanged(DeviceView view) {
				if (Platform.isFxApplicationThread()) {
					retextLabels();
					layoutDiagram();
				} else
					Platform.runLater(() -> viewChanged(view));
			}
		});
		componentType.addListener((e, o, n) -> rebuildElements());
		snapToGridCheckBox.selectedProperty().addListener((e, o, n) -> layoutDiagram());
		gridSizeSlider.valueProperty().addListener((e, o, n) -> layoutDiagram());

		rebuildComponentTypesPanel();
		rebuildElements();

		device.addListener(this);
	}

	@Override
	public void refresh() {
		if (Platform.isFxApplicationThread()) {
			layoutDiagram();
			if (isNeedImageChange())
				reloadImage();
			rebuildElements();
		} else
			Platform.runLater(() -> refresh());
	}

	public void setComponentType(ComponentType componentType) {
		this.componentType.set(componentType);
	}

	public void setLabelFactory(LabelFactory labelFactory) {
		this.labelFactory.set(labelFactory);
	}

	public final void setGraphicTextGap(double value) {
		graphicTextGapProperty().setValue(value);
	}

	public final void setKeySelectionModel(MultipleSelectionModel<IO> value) {
		keySelectionModel().set(value);
	}

	public final void setLineColor(Color lineColor) {
		lineColorProperty.setValue(lineColor);
	}

	public final void setLineWidth(double value) {
		lineWidthProperty().setValue(value);
	}

	public final void setSelectionModel(MultipleSelectionModel<IO> value) {
		keySelectionModel().set(value);
	}

	@Override
	public void updateFromMatrix(int[][][] frame) {
		if (Platform.isFxApplicationThread()) {
			DeviceView matrixView = null;
			for (ElementView elementView : elements.get()) {
				if (elementView.getElement() instanceof Area) {
					if (matrixView == null)
						matrixView = view.getLayout().getViews().get(ViewPosition.MATRIX);
					Area area = (Area) elementView.getElement();
					int[] rgb = new int[3];
					int r = 0;
					Region.Name region = area.getRegion();
					for (IO cell : matrixView.getElements()) {
						MatrixCell mc = (MatrixCell) cell;
						if (mc.getRegion() == region) {
							int[] rgbe = frame[mc.getMatrixY()][mc.getMatrixX()];
							if (rgbe != null) {
								rgb[0] += rgbe[0];
								rgb[1] += rgbe[1];
								rgb[2] += rgbe[2];
							}
							r++;
						}
					}
					if (r > 0) {
						elementView.getElementTool().setRGB(new int[] { rgb[0] / r, rgb[1] / r, rgb[2] / r });
					}
				} else if (elementView.getElement() instanceof MatrixIO) {
					MatrixIO matrixIO = (MatrixIO) elementView.getElement();
					int[] rgb = frame[matrixIO.getMatrixY()][matrixIO.getMatrixX()];
					if (rgb != null) {
						elementView.getElementTool().setRGB(rgb);
					}
				}
			}
		} else
			Platform.runLater(() -> updateFromMatrix(frame));
	}

	protected void moveSelection(double mx, double my) {
		Point2D imgSize = canvas.calcImageSize();
		if (snapToGridCheckBox.isSelected()) {
			double gw = Math.max(1, gridSizeSlider.valueProperty().get() * imgSize.getX());
			double gh = Math.max(1, gridSizeSlider.valueProperty().get() * imgSize.getY());
			mx *= gw;
			my *= gh;
		}
		for (IO el : getKeySelectionModel().getSelectedItems()) {
			el.setX((float) (el.getX() + ((1 / imgSize.getX()) * mx)));
			el.setY((float) (el.getY() + ((1 / imgSize.getY()) * my)));
		}
		layoutElements();
		layoutLabels();
		canvas.draw();
	}

	protected void addToSelection(IO led) {
		setFocused(true);
		int idx = items.indexOf(led);
		MultipleSelectionModel<IO> model = keySelectionModel().get();
		if (!model.getSelectedIndices().contains(idx)) {
			model.select(idx);
		}
	}

	protected void clearSelection() {
		getKeySelectionModel().clearSelection();
	}

	protected ElementView createElementView(ComponentType type) {
		ElementView elementView = new ElementView(type);
		Tool elementTool = new Tool(elementView, this);
		elementView.setElementTool(elementTool);
		return elementView;
	}

	protected Node createLabel(IO el) {
		LabelFactory factory = labelFactory.get();
		if (factory != null) {
			Node node = factory.createLabel(el);
			if (node != null)
				return node;
		}
		Label label = new Label(el.getLabel());
		label.setOnMouseClicked((e) -> {
			if (e.isControlDown())
				addToSelection(el);
			else
				selectSingle(el);
			e.consume();
		});
		label.getStyleClass().add("layout-label");
		return label;
	}

	protected String findBestDefaultLabelText(ComponentType type) {
		int highest = 0;
		for (ElementView element : elements.get()) {
			if (element.getType() == type) {
				int number = -1;
				try {
					number = Integer.parseInt(element.getElement().getLabel());
				} catch (Exception nfe) {
				}
				highest = Math.max(highest, number);
			}
		}
		return String.valueOf(highest + 1);
	}

	protected ElementView forElement(IO element) {
		for (ElementView e : elements.get()) {
			if (e.getElement().equals(element))
				return e;
		}
		return null;
	}

	protected String getFinalImageUrl() {
		if (view.getImageUri() == null) {
			BrandingImage bimg = view.getPosition().toBrandingImage();
			if (bimg != null) {
				return device.getImageUrl(bimg);
			} else
				return null;
		} else {
			return view.getResolvedImageUri(view.getLayout().getBase());
		}
	}

	protected double getTotalHeight(List<ElementView> top) {
		double totalTextHeight = 0;
		for (ElementView el : top) {
			if (totalTextHeight > 0)
				totalTextHeight += getGraphicTextGap();
			totalTextHeight += el.getLabel().getBoundsInLocal().getHeight();
		}
		return totalTextHeight;
	}

	protected double getTotalWidth(List<ElementView> top) {
		double totalTextWidth = 0;
		for (ElementView el : top) {
			if (totalTextWidth > 0)
				totalTextWidth += getGraphicTextGap();
			totalTextWidth += el.getLabel().getBoundsInLocal().getWidth();
		}
		return totalTextWidth;
	}

	protected boolean isLayoutReadOnly() {
		return view.getLayout().isReadOnly() || viewer.isReadOnly();
	}

	protected boolean isLayoutSelectableElements() {
		return viewer.isSelectableElements();
	}

	protected boolean isNeedImageChange() {
		return !Objects.equals(getFinalImageUrl(), loadedImage);
	}

	protected void layoutComponentTypes() {
		Insets insets = pane.getInsets();
		double y = insets.getTop();

		/* Find max best width and height of the radio buttons and icons */
		double w = 0;
		double h = 0;
		double rh = 0;
		double ih = 0;

		for (int i = 0; i < componentTypes.size(); i += 2) {
			Node n = componentTypes.get(i);
			w = Math.max(n.getLayoutBounds().getWidth() + getGraphicTextGap(), w);
			rh = Math.max(n.getLayoutBounds().getHeight() + getGraphicTextGap(), rh);
			n = componentTypes.get(i + 1);
			ih = Math.max(n.getLayoutBounds().getHeight() + getGraphicTextGap(), i);
		}
		h = Math.max(rh, h);

		/* Layout */
		for (int i = 0; i < componentTypes.size(); i++) {
			Node n = componentTypes.get(i);
			if (i % 2 == 1) {
				n.setLayoutY(y - ((ih - rh) / 2.0));
				n.setLayoutX(insets.getLeft() + w);
				y += h;
			} else {
				n.setLayoutY(y);
				n.setLayoutX(insets.getLeft());
			}
		}

		/* Redraw all of the graphics */
		for (Node node : componentTypes) {
			if (node instanceof Tool) {
				((Tool) node).redraw();
			}
		}

		/* Grid stuff */
		snapToGridCheckBox.setLayoutX(insets.getLeft());
		snapToGridCheckBox.setLayoutY(y);
		y += snapToGridCheckBox.getLayoutBounds().getHeight() + getGraphicTextGap();
		gridSizeSlider.setLayoutX(insets.getLeft());
		gridSizeSlider.setLayoutY(y);
	}

	protected void layoutDiagram() {

		/* Need this to make sure elements their preferred sizes */
		applyCss();
		layout();

		/*
		 * Layout actual nodes first. Then draw connecting lines and other decoration
		 */

		/* Elements are place exactly where specified */
		layoutElements();

		/* Need this to make sure labels have their preferred sizes */
		applyCss();
		layout();

		/* Arrange labels */
		layoutLabels();

		/* Arrange component types */
		layoutComponentTypes();

		/* Now draw background image, lines etc */
		canvas.draw();
	}

	protected void layoutElements() {
		Insets insets = pane.getInsets();
		double canvasWidth = widthProperty().get() - insets.getLeft() - insets.getRight();
		double canvasHeight = heightProperty().get() - insets.getTop() - insets.getBottom();
		canvas.widthProperty().set(canvasWidth);
		canvas.heightProperty().set(canvasHeight);
		canvas.layoutXProperty().set(insets.getLeft());
		canvas.layoutYProperty().set(insets.getTop());

		Point2D imgSize = canvas.calcImageSize();
		Point2D imgOff = canvas.calcImageOffset();
		for (ElementView elementView : elements.get()) {
			positionElement(imgSize, imgOff, elementView);
		}
	}

	protected void layoutLabels() {
		double canvasWidth = canvas.widthProperty().get();
		double canvasHeight = canvas.heightProperty().get();
		Insets insets = pane.getInsets();

		List<ElementView> left = new ArrayList<>();
		List<ElementView> top = new ArrayList<>();
		List<ElementView> bottom = new ArrayList<>();
		List<ElementView> right = new ArrayList<>();
		separateElements(left, top, bottom, right);

		// double y = ((canvasHeight - (rowHeight * left.size())) / 2f);
		double totalHeight = getTotalHeight(left);
		double y = ((canvasHeight - (totalHeight)) / 2f);

		/* Left */
		for (ElementView elementView : left) {
			double rowHeight = elementView.getLabel().getLayoutBounds().getHeight();
			positionLabelForElement(insets.getLeft(), y + insets.getTop(), elementView);
			y += rowHeight;
		}

		/* Right */
		// y = ((canvasHeight - (rowHeight * right.size())) / 2f);
		totalHeight = getTotalHeight(right);
		y = ((canvasHeight - (totalHeight)) / 2f);
		for (ElementView elementView : right) {
			double rowHeight = elementView.getLabel().getLayoutBounds().getHeight();
			positionLabelForElement(
					insets.getLeft() + canvasWidth - elementView.getLabel().getBoundsInLocal().getWidth(),
					y + insets.getTop(), elementView);
			y += rowHeight;
		}

		/* Top */
		double totalTextWidth = getTotalWidth(top);

		double x = ((canvasWidth - totalTextWidth) / 2f);
		for (ElementView elementView : top) {
			positionLabelForElement(x + insets.getLeft(), insets.getTop(), elementView);
			x += elementView.getLabel().getBoundsInLocal().getWidth() + getGraphicTextGap();
		}

		/* Bottom */
		totalTextWidth = getTotalWidth(bottom);
		x = ((canvasWidth - totalTextWidth) / 2f);
		for (ElementView elementView : bottom) {
			double rowHeight = elementView.getLabel().getLayoutBounds().getHeight();
			positionLabelForElement(x + insets.getLeft(), insets.getTop() + canvasHeight - rowHeight, elementView);
			x += elementView.getLabel().getBoundsInLocal().getWidth() + getGraphicTextGap();
		}
	}

	protected void positionElement(ElementView elementView) {
		positionElement(canvas.calcImageSize(), canvas.calcImageOffset(), elementView);
	}

	protected void positionElement(Point2D imgSize, Point2D imgOff, ElementView elementView) {
		Insets insets = pane.getInsets();
		Node node = elementView.getElementTool();
		double x = insets.getLeft() + imgOff.getX() + (imgSize.getX() * elementView.getElement().getX())
				- (node.layoutBoundsProperty().get().getWidth() / 2f);
		node.layoutXProperty().set(x);
		double y = insets.getTop() + imgOff.getY() + (imgSize.getY() * elementView.getElement().getY())
				- (node.layoutBoundsProperty().get().getHeight() / 2f);
		node.layoutYProperty().set(y);
		elementView.redraw();
	}

	protected void positionLabelForElement(double x, double y, ElementView element) {
		Node node = element.getLabel();
		node.layoutXProperty().set(x);
		node.layoutYProperty().set(y);
	}

	protected void reloadImage() {
		String requiredImage = getFinalImageUrl();
		img = requiredImage == null || requiredImage.equals("") ? null
				: new Image(context.getCache().getCachedImage(requiredImage), 0, 0, true, true);
		loadedImage = requiredImage;
	}

	protected void resnap() {
		for (ElementView e : elements.get()) {
			IO el = e.getElement();
			el.setX((float) (el.getX() - (el.getX() % gridSizeSlider.getValue()) + (gridSizeSlider.getValue() / 2.0)));
			el.setY((float) (el.getY() - (el.getY() % gridSizeSlider.getValue()) + (gridSizeSlider.getValue() / 2.0)));
		}
		layoutElements();
	}

	protected void retextLabels() {
		for (ElementView elementView : elements.get()) {
			if (elementView.getLabel() != null)
				if (elementView.getLabel() instanceof Label)
					((Label) elementView.getLabel()).textProperty().set(elementView.getElement().getLabel());
				else
					/* Recreate entirely if its not a label */
					elementView.setLabel(createLabel(elementView.getElement()));
		}
		pane.applyCss();
		pane.layout();
	}

	protected void selectSingle(IO led) {
		MultipleSelectionModel<IO> model = keySelectionModel.get();
		model.clearAndSelect(items.indexOf(led));
	}

	protected void separateElements(List<ElementView> left, List<ElementView> top, List<ElementView> bottom,
			List<ElementView> right) {
		for (ElementView elementView : elements.get()) {
			IO el = elementView.getElement();
			if (el.getY() < 0.4 && el.getX() >= 0.4 && el.getX() < 0.6) {
				top.add(elementView);
			} else if (el.getY() > 0.6 && el.getX() >= 0.4 && el.getX() < 0.6) {
				bottom.add(elementView);
			} else if (el.getX() <= 0.5) {
				left.add(elementView);
			} else {
				right.add(elementView);
			}
		}
		Collections.sort(left, (el1, el2) -> {
			return Float.valueOf(el1.getElement().getY()).compareTo(el2.getElement().getY());
		});
		Collections.sort(right, (el1, el2) -> {
			return Float.valueOf(el1.getElement().getY()).compareTo(el2.getElement().getY());
		});
		Collections.sort(top, (el1, el2) -> {
			return Float.valueOf(el1.getElement().getX()).compareTo(el2.getElement().getX());
		});
		Collections.sort(bottom, (el1, el2) -> {
			return Float.valueOf(el1.getElement().getX()).compareTo(el2.getElement().getX());
		});
	}

	void rebuildComponentTypesPanel() {
		for (Node c : componentTypes) {
			pane.getChildren().remove(c);
		}
		componentTypes.clear();
		ToggleGroup group = new ToggleGroup();
		Map<ComponentType, Tool> types = new HashMap<>();

		List<ComponentType> typesToShow = enabledTypes.get();
		if (typesToShow.size() > 1 || !isLayoutReadOnly()) {
			for (ComponentType type : typesToShow) {
				ElementView elementView = createElementView(type);
				RadioButton r = new RadioButton(TabbedViewer.bundle.getString("componentTypeMenu." + type.name()));
				r.getStyleClass().add("layout-selector-" + type.name());
				r.setToggleGroup(group);
				componentTypes.add(r);
				componentTypes.add(elementView.getElementTool());
				if (type == getComponentType()) {
					r.selectedProperty().set(true);
				}
				if (type != getComponentType() || isLayoutReadOnly()) {
					elementView.getElementTool().disableProperty().set(true);
				}

				r.selectedProperty().addListener((e) -> {
					setComponentType(type);
					for (Map.Entry<ComponentType, Tool> en : types.entrySet()) {
						if (!isLayoutReadOnly() && en.getKey() == type && !isLayoutReadOnly())
							en.getValue().disableProperty().set(false);
						else
							en.getValue().disableProperty().set(true);

					}
				});
				types.put(type, elementView.getElementTool());
			}
		}
		for (Node c : componentTypes) {
			pane.getChildren().add(c);
		}
		applyCss();
		layout();
		layoutComponentTypes();
	}

	private void rebuildElements() {
		/* Remove all existing labels and elements */
		for (ElementView e : elements.get()) {
			pane.getChildren().remove(e.getElementTool());
			if (e.getLabel() != null)
				pane.getChildren().remove(e.getLabel());

		}
		elements.get().clear();

		applyCss();
		layout();
		if (view != null) {
			for (IO io : view.getElements()) {
				ElementView ev = null;
				if (io instanceof LED && getComponentType() == ComponentType.LED) {
					ev = createElementView(ComponentType.LED);
				} else if (io instanceof Key && getComponentType() == ComponentType.KEY) {
					ev = createElementView(ComponentType.KEY);
				} else if (io instanceof Area && getComponentType() == ComponentType.AREA) {
					ev = createElementView(ComponentType.AREA);
				}
				if (ev != null) {
					ev.setElement(io);
					ev.setLabel(createLabel(io));
					elements.get().add(ev);
					pane.getChildren().add(ev.getLabel());
					pane.getChildren().add(ev.getElementTool());
				}

			}
			layoutDiagram();
		}
	}

	@Override
	public DeviceView getView() {
		return view;
	}

	public static String getBestRegionName(DeviceView view, Name name) {
		IO regionEl = view.getAreaElement(name);
		if (regionEl != null && regionEl.getLabel() != null)
			return regionEl.getLabel();
		return Strings.toName(name.toString());
	}

	public void reset() {
		for(ElementView element : elements.get()) {
			element.reset();
		}
	}

}