package uk.co.bithatch.snake.ui.designer;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.Region.Name;
import uk.co.bithatch.snake.lib.layouts.Area;
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.DeviceView;
import uk.co.bithatch.snake.lib.layouts.IO;
import uk.co.bithatch.snake.lib.layouts.MatrixCell;
import uk.co.bithatch.snake.lib.layouts.MatrixIO;
import uk.co.bithatch.snake.lib.layouts.ViewPosition;
import uk.co.bithatch.snake.ui.graphics.AbstractGraphic;
import uk.co.bithatch.snake.ui.graphics.AccessoryGraphic;
import uk.co.bithatch.snake.ui.graphics.AreaGraphic;
import uk.co.bithatch.snake.ui.graphics.KeyGraphic;
import uk.co.bithatch.snake.ui.graphics.LEDGraphic;
import uk.co.bithatch.snake.ui.util.Delta;
import uk.co.bithatch.snake.ui.util.JavaFX;
import uk.co.bithatch.snake.ui.util.Maths;
import uk.co.bithatch.snake.ui.util.Strings;

public class Tool extends StackPane {
	private final Delta dragDelta = new Delta();
	private double startX = -1, startY = -1;
	private Map<ElementView, Point2D> selectedAtDragStart = new HashMap<>();
	private ElementView elementView;
	private LayoutEditor deviceViewerPane;
	private Node graphic;

	public Tool(ElementView elementView, LayoutEditor deviceViewerPane) {
		super();
		this.elementView = elementView;
		this.deviceViewerPane = deviceViewerPane;

		if (deviceViewerPane.isShowElementGraphics()) {
			if (elementView.getType() == ComponentType.LED)
				graphic = new LEDGraphic();
			else if (elementView.getType() == ComponentType.KEY)
				graphic = new KeyGraphic();
			else if (elementView.getType() == ComponentType.AREA)
				graphic = new AreaGraphic();
			else if (elementView.getType() == ComponentType.ACCESSORY)
				graphic = new AccessoryGraphic();
			else
				throw new UnsupportedOperationException();
		} else {
			graphic = new Label(TabbedViewer.bundle.getString("dot"));
			graphic.getStyleClass().add("smallIcon");
		}
		getChildren().add(graphic);

		ComponentType type = elementView.getType();
		getStyleClass().add("layout-tool");
		getStyleClass().add("layout-tool-" + type.name().toLowerCase());
		setOnMousePressed((e) -> startDrag(e));
		setOnMouseReleased((e) -> endDrag(e));
		setOnMouseDragged((e) -> dragMovement(e));
		setOnMouseEntered(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (!deviceViewerPane.isLayoutReadOnly()) {
					setCursor(Cursor.HAND);
				}
			}
		});
	}

	public void setRGB(int[] rgb) {
		if (graphic instanceof AbstractGraphic)
			((AbstractGraphic) graphic).setLedColor(JavaFX.toColor(rgb));
		else
			graphic.setStyle("-fx-text-fill: " + JavaFX.toHex(rgb));
	}

	void startDrag(MouseEvent mouseEvent) {
		if (deviceViewerPane.isLayoutSelectableElements()) {
			selectedAtDragStart.clear();
			for (ElementView ev : deviceViewerPane.elements.get()) {
				if (deviceViewerPane.getElementSelectionModel().getSelectedItems().contains(ev.getElement())) {
					selectedAtDragStart.put(ev,
							new Point2D(ev.getElementTool().getLayoutX(), ev.getElementTool().getLayoutY()));
				}
			}

			startX = getLayoutX();
			startY = getLayoutY();
			dragDelta.setX(startX - mouseEvent.getSceneX());
			dragDelta.setY(startY - mouseEvent.getSceneY());
			setCursor(Cursor.MOVE);

			deviceViewerPane.requestFocus();

			/*
			 * If this is different from the current selection, and not a mulitple selection
			 * (i.e. shift or ctrl modifier), then select the new element now
			 */
			if (deviceViewerPane.isLayoutSelectableElements() && !mouseEvent.isControlDown()
					&& !mouseEvent.isShiftDown() && !deviceViewerPane.getElementSelectionModel().getSelectedItems()
							.contains(elementView.getElement())) {
				selectedAtDragStart.clear();
				deviceViewerPane.selectSingle(elementView.getElement());
			}

			mouseEvent.consume();
		}
	}

	void dragMovement(MouseEvent mouseEvent) {
		if (startX > -1 && startY > -1) {

			int idx = deviceViewerPane.componentTypes.indexOf(Tool.this);
			if (idx != -1) {

				/*
				 * Creating a new element. Remove this tool from the list so it doesnt get
				 * removed from the scene
				 */
				deviceViewerPane.componentTypes.remove(idx);
				deviceViewerPane.getChildren().remove(deviceViewerPane.componentTypes.remove(idx - 1));

				deviceViewerPane.rebuildComponentTypesPanel();

				/*
				 * Attach a label to this element view. Look for a label and region from the
				 * matrix view if there is one.
				 */
//				String label = deviceViewerPane.findBestDefaultLabelText(elementView.getType());
//
//				if (elementView.getElement() instanceof Area) {
//					Area area = (Area) elementView.getElement();
//					Set<Region.Name> available = new LinkedHashSet<>(deviceViewerPane.device.getRegionNames());
//					for (IO el : deviceViewerPane.view.getElements()) {
//						if (el instanceof Area) {
//							Area a = (Area) el;
//							if (a.getRegion() != null)
//								available.remove(a.getRegion());
//						}
//					}
//					if (!available.isEmpty()) {
//						Name a = available.iterator().next();
//						area.setRegion(a);
//						label = Strings.toName(a.name());
//					}
//				}

				if (elementView.getElement() instanceof MatrixIO) {
					MatrixIO mio = (MatrixIO) elementView.getElement();

					try {
						((MatrixIO) mio).setMatrixXY(
								deviceViewerPane.view.getNextFreeCell(deviceViewerPane.componentType().get()));
					} catch (IllegalStateException ise) {
					}

//					DeviceView matrixView = deviceViewerPane.view.getLayout().getViews().get(ViewPosition.MATRIX);
//					if (matrixView != null) {
//						MatrixCell otherElement = matrixView.getElement(ComponentType.MATRIX_CELL, mio.getMatrixX(),
//								mio.getMatrixY());
//						if (otherElement != null && otherElement.getLabel() != null) {
//							label = otherElement.getLabel();
//						}
//					}
				}
//				elementView.getElement().setLabel(label);
				elementView.setLabel(deviceViewerPane.createLabel(elementView.getElement()));
				deviceViewerPane.pane.getChildren().add(elementView.getLabel());
				deviceViewerPane.elements.get().add(elementView);

				/*
				 * Needed to make sure the above label has proper sizes to be able to calculate
				 * line positions on first paint.
				 */
				applyCss();
				layout();
			} else if (deviceViewerPane.isLayoutReadOnly())
				return;

			setLayoutX(mouseEvent.getSceneX() + dragDelta.getX());
			setLayoutY(mouseEvent.getSceneY() + dragDelta.getY());
			double mx = getLayoutX() - startX;
			double my = getLayoutY() - startY;

			for (Map.Entry<ElementView, Point2D> en : selectedAtDragStart.entrySet()) {
				if (en.getKey() != elementView) {
					deviceViewerPane.canvas.updateElement(en.getKey(), en.getValue().getX() + mx,
							en.getValue().getY() + my);
					deviceViewerPane.positionElement(en.getKey());
				}
			}

			Insets insets = deviceViewerPane.pane.getInsets();
			double toolX = layoutXProperty().get() + (layoutBoundsProperty().get().getWidth() / 2f);
			double toolY = layoutYProperty().get() + (layoutBoundsProperty().get().getHeight() / 2f);

			deviceViewerPane.canvas.updateElement(elementView, toolX - insets.getLeft(), toolY - insets.getTop());

			deviceViewerPane.layoutLabels();
			deviceViewerPane.canvas.draw();
		}
	}

	void endDrag(MouseEvent mouseEvent) {
		if (startX > -1 && startY > -1) {

			double endX = layoutXProperty().get();
			double endY = layoutYProperty().get();

			double dist = Maths.distance(startX, startY, endX, endY);
			IO element = elementView.getElement();
			if (dist < 30) {
				if (!deviceViewerPane.view.getElements().contains(element)) {

					if (deviceViewerPane.isLayoutSelectableElements())
						deviceViewerPane.clearSelection();

					deviceViewerPane.elements.get().remove(elementView);
					deviceViewerPane.getChildren().remove(elementView.getElementTool());
					deviceViewerPane.getChildren().remove(elementView.getLabel());
					deviceViewerPane.rebuildComponentTypesPanel();
					deviceViewerPane.layoutDiagram();
				} else {
					if (deviceViewerPane.isLayoutSelectableElements() && mouseEvent.isControlDown()) {
						deviceViewerPane.addToSelection(elementView.getElement());
					}

				}
			} else {
				Insets insets = deviceViewerPane.pane.getInsets();
				double toolX = endX + (layoutBoundsProperty().get().getWidth() / 2f);
				double toolY = endY + (layoutBoundsProperty().get().getHeight() / 2f);
				deviceViewerPane.canvas.updateElement(elementView, toolX - insets.getLeft(), toolY - insets.getTop());

				if (!deviceViewerPane.view.getElements().contains(element)) {
					deviceViewerPane.view.addElement(element);
					deviceViewerPane.addToSelection(element);
				}

				deviceViewerPane.layoutLabels();
				deviceViewerPane.canvas.draw();
			}
			setCursor(Cursor.HAND);
			startX = startY = -1;
		}
		mouseEvent.consume();
	}

	public void redraw() {
		if (graphic instanceof AbstractGraphic)
			((AbstractGraphic) graphic).draw();
	}

	public void setSelected(boolean selected) {
		if (graphic instanceof AbstractGraphic)
			((AbstractGraphic) graphic).setSelected(selected);
	}

	public void reset() {
		if (graphic instanceof AbstractGraphic) {
			((AbstractGraphic) graphic).setLedColor(null);
		} else
			graphic.setStyle(null);
	}
}