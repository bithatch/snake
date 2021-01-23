package uk.co.bithatch.snake.widgets;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty; 
import javafx.beans.value.ObservableValue;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableObjectProperty;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.event.EventHandler;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class SelectableArea extends StackPane {
	private static final StyleablePropertyFactory<SelectableArea> FACTORY = new StyleablePropertyFactory<>(
			StackPane.getClassCssMetaData());

	private static final CssMetaData<SelectableArea, Color> OUTLINE_COLOR = FACTORY
			.createColorCssMetaData("-snake-outline-color", s -> s.outlineColorProperty, Color.GRAY, true);

	private final StyleableProperty<Color> outlineColorProperty = new SimpleStyleableObjectProperty<>(OUTLINE_COLOR,
			this, "outlineColor");

	private ObjectProperty<BoundingBox> selectableArea = new SimpleObjectProperty<>(null, "selectableArea");
	private Point2D dragStart;
	private SelectPane selectPane;
	private DragWindow dragWindow;
	private EventHandler<MouseEvent> defaultClickHandler;
	private Node content;
	private final BooleanProperty selectableProperty = new SimpleBooleanProperty(true);

	public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
		return FACTORY.getCssMetaData();
	}

	public SelectableArea() {
		getStyleClass().add("selectable-area");
		selectPane = new SelectPane();
		dragWindow = new DragWindow();
		getChildren().add(selectPane);
		getChildren().add(dragWindow);

		dragWindow.widthProperty().bind(widthProperty());
		dragWindow.heightProperty().bind(heightProperty());

		selectableProperty.addListener((e, o, n) -> {
			if (!n && getSelectableArea() != null)
				setSelectableArea(null);
		});
	}

	public EventHandler<MouseEvent> getDefaultClickHandler() {
		return defaultClickHandler;
	}

	public void setDefaultClickHandler(EventHandler<MouseEvent> defaultClickHandler) {
		this.defaultClickHandler = defaultClickHandler;
	}

	public void setContent(Node content) {
		if (this.content != null)
			getChildren().remove(content);
		getChildren().add(1, content);
		this.content = content;
	}

	public ObjectProperty<BoundingBox> selectableArea() {
		return selectableArea;
	}

	public BoundingBox getSelectableArea() {
		return selectableArea.get();
	}

	public void setSelectableArea(BoundingBox selection) {
		this.selectableArea.set(selection);
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

	public final void setOutlineColor(Color lineColor) {
		outlineColorProperty.setValue(lineColor);
	}

	public final boolean isSelectable() {
		return selectableProperty.getValue();
	}

	public BooleanProperty selectableProperty() {
		return selectableProperty;
	}

	public final void setSelectable(boolean selectable) {
		selectableProperty.setValue(selectable);
	}

	class SelectPane extends Pane {
		SelectPane() {
			setOnMousePressed((e) -> {
				if (isSelectable()) {
					dragStart = new Point2D(e.getX(), e.getY());
					dragWindow.draw();
				}
			});
			setOnMouseDragged((e) -> {
				if (dragStart == null)
					/* Can get a drag without getting a press */
					return;

				BoundingBox current = selectableArea.get();
				double currX = dragStart.getX();
				double currY = dragStart.getY();
				double evtX = e.getX();
				double evtY = e.getY();
				if (current == null) {
					double dist = distance(currX, currY, evtX, evtY);
					if (dist < 10) {
						/* Not yet enough to start drag */
						return;
					}
				}
				double szw = evtX - currX;
				double szh = evtY - currY;
				if (szw < 0) {
					currX = evtX;
					szw = current == null ? 0 : current.getMaxX() - currX;
				}
				if (szh < 0) {
					currY = evtY;
					szh = current == null ? 0 : current.getMaxY() - currY;
				}
				setSelectableArea(new BoundingBox(currX, currY, szw, szh));
				dragWindow.draw();
				e.consume();
			});
			setOnMouseReleased((e) -> {
				if (getSelectableArea() != null) {
					dragStart = null;
					setSelectableArea(null);
					dragWindow.draw();
					e.consume();
				} else if (defaultClickHandler != null)
					defaultClickHandler.handle(e);
			});
		}
	}

	class DragWindow extends Canvas {

		DragWindow() {
			super(8, 8);
			draw();
			setMouseTransparent(true);
			widthProperty().addListener(evt -> draw());
			heightProperty().addListener(evt -> draw());
		}

		@Override
		public boolean isResizable() {
			return true;
		}

		@Override
		public double minWidth(double height) {
			return content == null ? 0 : content.minWidth(height);
		}

		@Override
		public double minHeight(double width) {
			return content == null ? 0 : content.minHeight(width);
		}

		@Override
		public double prefWidth(double height) {
			return content == null ? 0 : content.prefWidth(height);
		}

		@Override
		public double prefHeight(double width) {
			return content == null ? 0 : content.prefHeight(width);
		}

		protected void draw() {
			GraphicsContext ctx = getGraphicsContext2D();

			double canvasWidth = boundsInLocalProperty().get().getWidth();
			double canvasHeight = boundsInLocalProperty().get().getHeight();

			ctx.clearRect(0, 0, canvasWidth, canvasHeight);

			BoundingBox rect = selectableArea.get();
			if (rect != null) {
				Color col = getOutlineColor();

				ctx.setFill(col);
				ctx.setGlobalAlpha(0.25);
				ctx.fillRect(rect.getMinX(), rect.getMinY(), rect.getWidth(), rect.getHeight());
				ctx.setGlobalAlpha(1);

				ctx.setStroke(col);
				ctx.beginPath();
				ctx.setLineWidth(0.5);
				ctx.moveTo(rect.getMinX(), rect.getMinY());
				ctx.lineTo(rect.getMinX(), rect.getMaxY());
				ctx.lineTo(rect.getMaxX(), rect.getMaxY());
				ctx.lineTo(rect.getMaxX(), rect.getMinY());
				ctx.lineTo(rect.getMinX(), rect.getMinY());
				ctx.stroke();
				ctx.closePath();
			}
		}
	}

	static double distance(double startX, double startY, double endX, double endY) {
		return Math.sqrt(Math.pow(endX - startX, 2) + Math.pow(endY - startY, 2));
	}
}
