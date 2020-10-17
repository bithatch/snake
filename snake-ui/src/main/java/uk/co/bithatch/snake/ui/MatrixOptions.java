package uk.co.bithatch.snake.ui;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point2D;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.effects.Matrix;
import uk.co.bithatch.snake.lib.layouts.Key;
import uk.co.bithatch.snake.lib.layouts.Layout;

public class MatrixOptions extends AbstractEffectController<uk.co.bithatch.snake.lib.effects.Matrix> {

	static class Cell {
		int row;
		int col;
		int[] rgb;

		Cell(int row, int col) {
			this(row, col, new int[3]);
		}

		Cell(int row, int col, int[] rgb) {
			this.row = row;
			this.col = col;
			this.rgb = rgb;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + row;
			result = prime * result + col;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Cell other = (Cell) obj;
			if (row != other.row)
				return false;
			if (col != other.col)
				return false;
			return true;
		}

		public void setRGB(int[] rgb) {
			this.rgb = rgb;
		}

		@Override
		public String toString() {
			return "Cell [row=" + row + ", col=" + col + ", rgb=" + Arrays.toString(rgb) + "]";
		}

	}

	final static int WIDTH = 32;
	final static int HEIGHT = 36;
	final static int HGAP = 14;
	final static int VGAP = 4;
	final static float ADJUST = 0.4f;

	@FXML
	private Label layout;

	@FXML
	private Label layoutLabel;

	@FXML
	private Label colorLabel;

	@FXML
	private ColorPicker color;

	@FXML
	private BorderPane container;
	@FXML
	private HBox scrollView;

	private KeyboardLayout matrix;

	private Map<Cell, ToggleButton> buttons = new HashMap<>();
	private boolean adjusting;
	private ToggleButton lastButton;
	private Cell lastCell;
	private Point2D dragStart = null;
	private boolean dragMoved = false;

	@Override
	protected void onSetEffectDevice() {
		layoutLabel.setLabelFor(layout);
		colorLabel.setLabelFor(color);

		EventHandler<? super MouseEvent> dragStartHandler = (e) -> {
			dragStart = new Point2D(e.getSceneX(), e.getSceneY());
			dragMoved = false;
			e.consume();
		};
		container.onMousePressedProperty().set(dragStartHandler);
		scrollView.onMousePressedProperty().set(dragStartHandler);
		EventHandler<? super MouseEvent> dragMoveHandler = (e) -> {
			var dragPos = new Point2D(e.getSceneX(), e.getSceneY());
			if (dragStart != null) {
				dragMoved = true;
				var bounds = new BoundingBox(dragStart.getX(), dragStart.getY(), dragPos.getX() - dragStart.getX(),
						dragPos.getY() - dragStart.getY());
				for (ToggleButton b : buttons.values()) {
					if (b.intersects(b.sceneToLocal(bounds)))
						b.selectedProperty().set(!e.isControlDown());
				}
			}
			e.consume();
		};
		container.onMouseDraggedProperty().set(dragMoveHandler);
		scrollView.onMouseDraggedProperty().set(dragMoveHandler);
		EventHandler<? super MouseEvent> dragEndHandler = (e) -> {
			dragStart = null;
			e.consume();
			if (!dragMoved)
				deselectAll();
			else
				dragMoved = false;
		};
		container.onMouseReleasedProperty().set(dragEndHandler);
		scrollView.onMouseReleasedProperty().set(dragEndHandler);
		matrix = new KeyboardLayout();
		scrollView.getChildren().add(matrix);
	}

	@Override
	protected void onSetEffect() {
		var effect = getEffect();

		matrix.getChildren().clear();
		int[] dim = getDevice().getMatrixSize();
		int y = dim[0];
		int x = dim[1];
		matrix.xProperty().set(x);
		matrix.yProperty().set(y);

		layoutLabel.managedProperty().bind(layoutLabel.visibleProperty());
		layout.managedProperty().bind(layout.visibleProperty());
		Layout layoutObj = null;
		if (getDevice().getCapabilities().contains(Capability.KEYBOARD_LAYOUT)) {
			layoutObj = Layout.get(getDevice().getKeyboardLayout(), getDevice().getMatrixSize()[1],
					getDevice().getMatrixSize()[0]);
			layout.textProperty().set(String.format("%s (%s)", layoutObj.getName(), layoutObj.getLayout()));
			layout.visibleProperty().set(true);
			layoutLabel.visibleProperty().set(true);
		} else {
			layout.visibleProperty().set(false);
			layoutLabel.visibleProperty().set(false);
		}

		for (int i = 0; i < y; i++) {
			for (int j = 0; j < x; j++) {
				ToggleButton but;
				if (layoutObj == null) {
					but = new ToggleButton();
					Label butLabel = new Label(String.valueOf((i * x) + j));
					butLabel.textOverrunProperty().set(OverrunStyle.CLIP);
					but.graphicProperty().set(butLabel);
				} else {
					Key key = layoutObj.getKeys()[i][j];
					but = new ToggleButton();
					Label butLabel = new Label(key == null || key.getLabel() == null ? "" : key.getLabel());
					butLabel.textOverrunProperty().set(OverrunStyle.CLIP);
					but.graphicProperty().set(butLabel);
					if (key != null && key.getWidth() > 0) {
						but.minWidthProperty().set(key.getWidth() * 0.30);
//						pw = (float)key.getWidth() * ADJUST;
					}
					if (key == null || key.isDisabled() || key.getLabel() == null)
						continue;
				}
				but.onMousePressedProperty().set((e) -> {
					dragStart = new Point2D(e.getSceneX(), e.getSceneY());
				});
				but.getStyleClass().add("key");
				KeyboardLayout.setCol(but, j);
				KeyboardLayout.setRow(but, i);

				int[] rgb = effect.getCell(i, j);
				var cell = new Cell(i, j, rgb);
				setButtonColor(cell, but, rgb);
				buttons.put(cell, but);
				but.setOnMouseReleased((e) -> {
					if (e.isShiftDown()) {
						if (lastButton != null) {
							int start = (x * cell.row) + cell.col;
							int end = (x * lastCell.row) + lastCell.col;
							if (start > end) {
								int o = end;
								end = start;
								start = o;
							}
							for (int ii = start; ii <= end; ii++) {
								int r = ii / x;
								Cell c = new Cell(r, ii - (r * x));
								ToggleButton b = buttons.get(c);
								if (b != null)
									b.selectedProperty().set(!but.selectedProperty().get());
							}
						}
					} else if (e.isControlDown())
						but.selectedProperty().set(!but.selectedProperty().get());
					else {
						for (ToggleButton other : buttons.values()) {
							if (other != but) {
								if (!e.isControlDown()) {
									other.selectedProperty().set(false);
								}
							}
						}
						setColorForButtons();
					}
					lastButton = but;
					lastCell = cell;
					dragStart = null;
					e.consume();
				});

				matrix.getChildren().add(but);
			}
		}
		matrix.layout();
	}

	void updateMatrix() {
		int[] dim = getDevice().getMatrixSize();
		int y = dim[0];
		int x = dim[1];
		try {
			Matrix matrix = (Matrix) getEffect().clone();
			int[][][] cells = new int[y][x][3];
			for (Cell cell : this.buttons.keySet()) {
				cells[cell.row][cell.col] = cell.rgb;
			}
			matrix.setCells(cells);
			context.getScheduler().execute(() -> getRegion().setEffect(matrix));
		} catch (CloneNotSupportedException e1) {
		}
	}

	@FXML
	void evtSelectAll(ActionEvent evt) {
		for (ToggleButton b : buttons.values())
			b.selectedProperty().set(true);
		setColorForButtons();
	}

	@FXML
	void evtReset(ActionEvent evt) {
		for (Map.Entry<Cell, ToggleButton> other : buttons.entrySet()) {
			if (other.getValue().selectedProperty().get()) {
				setButtonColor(other.getKey(), other.getValue(), new int[3]);
			}
		}
		deselectAll();
		updateMatrix();
		setColorForButtons();
	}

	private void deselectAll() {
		for (ToggleButton b : buttons.values())
			b.selectedProperty().set(false);
	}

	@FXML
	void evtColor(ActionEvent evt) {
		if (!adjusting) {
			for (Map.Entry<Cell, ToggleButton> other : buttons.entrySet()) {
				if (other.getValue().selectedProperty().get()) {
					setButtonColor(other.getKey(), other.getValue(), UIHelpers.toRGB(color.valueProperty().get()));
				}
			}
			updateMatrix();
		}
	}

	@FXML
	void evtBack(ActionEvent evt) {
		context.pop();
	}

	void setButtonColor(Cell cell, ToggleButton button, int[] rgb) {
		cell.setRGB(rgb);
		button.getGraphic().setStyle(String.format("-fx-text-fill: %s", UIHelpers.toHex(rgb)));
	}

	void setColorForButtons() {
		adjusting = true;
		try {
			int r = 0;
			int g = 0;
			int b = 0;
			int rows = 0;
			for (Map.Entry<Cell, ToggleButton> other : buttons.entrySet()) {
				if (other.getValue().selectedProperty().get()) {
					rows++;
					r += other.getKey().rgb[0];
					g += other.getKey().rgb[1];
					b += other.getKey().rgb[2];
				}
			}
			if (rows == 0)
				color.valueProperty().set(Color.BLACK);
			else
				color.valueProperty().set(UIHelpers.toColor(new int[] { r / rows, g / rows, b / rows }));
		} finally {
			adjusting = false;
		}
	}

}
