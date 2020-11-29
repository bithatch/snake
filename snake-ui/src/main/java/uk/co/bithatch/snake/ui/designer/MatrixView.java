package uk.co.bithatch.snake.ui.designer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Device.Listener;
import uk.co.bithatch.snake.lib.binding.Profile;
import uk.co.bithatch.snake.lib.binding.ProfileMap;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.layouts.Area;
import uk.co.bithatch.snake.lib.layouts.Cell;
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.DeviceLayout;
import uk.co.bithatch.snake.lib.layouts.DeviceView;
import uk.co.bithatch.snake.lib.layouts.IO;
import uk.co.bithatch.snake.lib.layouts.MatrixCell;
import uk.co.bithatch.snake.lib.layouts.MatrixIO;
import uk.co.bithatch.snake.lib.layouts.ViewPosition;
import uk.co.bithatch.snake.ui.KeyboardLayout;
import uk.co.bithatch.snake.ui.ListMultipleSelectionModel;
import uk.co.bithatch.snake.ui.util.BasicList;
import uk.co.bithatch.snake.ui.util.JavaFX;
import uk.co.bithatch.snake.ui.util.ListWrapper;
import uk.co.bithatch.snake.ui.widgets.SelectableArea;

public class MatrixView extends SelectableArea implements ViewerView, Listener {

	static class MatrixCellButton extends ToggleButton {

		private MatrixCell element;
		private boolean partialDisable;
		private int[] rgb;

		public MatrixCellButton(MatrixView view, MatrixCell element) {
			super();
			this.element = element;

			managedProperty().bind(visibleProperty());

			Label butLabel = new Label(element == null || element.getDisplayLabel() == null ? "" : element.getDisplayLabel());
			butLabel.textOverrunProperty().set(OverrunStyle.CLIP);
			graphicProperty().set(butLabel);

			if (element != null && element.getWidth() > 0) {
				minWidthProperty().set(element.getWidth() * 0.30);
			}
			if (element.isDisabled() || element.getLabel() == null) {
				if (view.isLayoutReadOnly())
					setVisible(false);
				else
					setPartialDisable(true);
			}
			getStyleClass().add("key");

			var cell = element.getMatrixXY();
			setRgb(Colors.COLOR_BLACK);
			setOnMouseReleased((e) -> {

				int deviceX = Integer.valueOf(view.view.getLayout().getMatrixWidth());

				if (e.isShiftDown()) {
					if (view.lastButton != null) {
						int start = (deviceX * cell.getY()) + cell.getX();
						int end = (deviceX * view.lastCell.getY()) + view.lastCell.getX();
						if (start > end) {
							int o = end;
							end = start;
							start = o;
						}
						for (int ii = start; ii <= end; ii++) {
							int r = ii / deviceX;
							Cell c = new Cell(ii - (r * deviceX), r);
							MatrixCellButton b = view.buttons.get(c);
							if (b != null) {
								if (ii == start)
									view.select(b.getElement());
								else
									view.toggleSelection(b.getElement());
							}
						}
					}
				} else if (e.isControlDown())
					view.toggleSelection(element);
				else {
					view.select(element);
				}

				view.updateAvailability();
				view.lastButton = this;
				view.lastCell = cell;
				e.consume();
			});
		}

		public MatrixCellButton(String text) {
			super(text);
		}

		public MatrixCellButton(String text, Node graphic) {
			super(text, graphic);
		}

		public MatrixCell getElement() {
			return element;
		}

		public int[] getRgb() {
			return rgb;
		}

		public boolean isPartialDisable() {
			return partialDisable;
		}

		public void setPartialDisable(boolean partialDisable) {
			this.partialDisable = partialDisable;
			setOpacity(partialDisable ? 0.5 : 1);
		}

		public void setRgb(int[] rgb) {
			this.rgb = rgb;
			getGraphic().setStyle(String.format("-fx-text-fill: %s", JavaFX.toHex(rgb)));
		}

	}

	private Map<Cell, MatrixCellButton> buttons = new HashMap<>();
	private Device device;
	private Integer deviceX;
	private Integer deviceY;
	private ObjectProperty<ObservableList<MatrixCellButton>> elements = new SimpleObjectProperty<>(this, "elements");
	private ListWrapper<MatrixCellButton, IO> items;
	private Pane keyContainer;
	private ObjectProperty<MultipleSelectionModel<IO>> keySelectionModel = new SimpleObjectProperty<MultipleSelectionModel<IO>>(
			this, "keySelectionModel");
	private MatrixCellButton lastButton;
	private Cell lastCell;
	private DeviceView view;
//	private ScrollPane scroll;
	private Viewer viewer;

	{
		// TODO will need when add back support for keyboard matrixes
//		scroll = new ScrollPane();
//		scroll.getStyleClass().add("focusless");
//		scroll.getStyleClass().add("transparentBackground");
//		scroll.setPickOnBounds(true);
//		setContent(scroll);

		elements.set(new BasicList<MatrixCellButton>());
		items = new ListWrapper<MatrixCellButton, IO>(elements.get()) {
			@Override
			protected IO doConvertToWrapped(MatrixCellButton in) {
				return in.getElement();
			}
		};
		setDefaultClickHandler((e) -> clearSelection());
		setKeySelectionModel(new ListMultipleSelectionModel<>(items));
		setOnMouseClicked((e) -> {
			requestFocus();
			setFocused(true);
		});
		setFocusTraversable(true);
		setOnKeyReleased((e) -> {
			if (e.isControlDown() && e.getCode() == KeyCode.A) {
				getElementSelectionModel().selectAll();
			}
		});
		getElementSelectionModel().getSelectedIndices().addListener(new ListChangeListener<Integer>() {
			@Override
			public void onChanged(Change<? extends Integer> c) {
				while (c.next()) {
					if (c.wasRemoved()) {
						for (Integer i : c.getRemoved()) {
							MatrixCellButton button = elements.get().get(i);
							button.selectedProperty().set(false);
						}
					}
					if (c.wasAdded()) {
						for (Integer i : c.getAddedSubList()) {
							MatrixCellButton button = elements.get().get(i);
							button.selectedProperty().set(true);
						}
					}
				}
			}
		});

		selectableArea().addListener((e, o, n) -> {
			if (o == null && n != null) {
				clearSelection();
			}
			if (n != null) {
				for (MatrixCellButton b : elements.get()) {
					if (n.intersects(b.getBoundsInParent())) {
						addToSelection(b.getElement());
					}
				}
			}
		});

	}

	@Override
	public void changed(Device device, Region region) {
	}

	@Override
	public void close() throws Exception {
		device.removeListener(this);
	}

	public void deselectAll() {
		for (MatrixCellButton b : buttons.values())
			b.selectedProperty().set(false);
		updateAvailability();
	}

	@Override
	public List<IO> getElements() {
		return items;
	}

	public final MultipleSelectionModel<IO> getElementSelectionModel() {
		return keySelectionModel == null ? null : keySelectionModel.get();
	}

	@Override
	public Node getRoot() {
		return this;
	}

	public final ObjectProperty<MultipleSelectionModel<IO>> keySelectionModel() {
		return keySelectionModel;
	}

	@Override
	public void open(Device device, DeviceView view, Viewer viewer) {
		this.view = view;
		this.device = device;
		this.viewer = viewer;

		deviceX = Integer.valueOf(view.getLayout().getMatrixWidth());
		deviceY = Integer.valueOf(view.getLayout().getMatrixHeight());

		if (deviceY == 1) {
			/*
			 * If a single row matrix, then wrap in flow pane so we can show everything
			 * without any scrolling
			 */
			FlowPane flow = new FlowPane();
			flow.getStyleClass().add("spaced");
			getStyleClass().add("single-row-matrix");
			flow.setAlignment(Pos.CENTER);
//			scroll.setFitToHeight(true);
//			scroll.setFitToWidth(true);
			keyContainer = flow;
//			scroll.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
//			scroll.setVbarPolicy(ScrollBarPolicy.NEVER);

		} else {
			KeyboardLayout layout = new KeyboardLayout();
			getStyleClass().add("multi-row-matrix");
			layout.xProperty().set(deviceX);
			layout.yProperty().set(deviceY);
//			scroll.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
//			scroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);

			keyContainer = layout;
		}

		rebuildElements();

//		scroll.setContent(keyContainer);
		keyContainer.setPickOnBounds(false);
//		scroll.setPickOnBounds(false);
		setContent(keyContainer);

		device.addListener(this);
		view.addListener(new DeviceView.Listener() {
			@Override
			public void viewChanged(DeviceView view) {
				if (Platform.isFxApplicationThread())
					retextLabels();
				else
					Platform.runLater(() -> viewChanged(view));
			}

			@Override
			public void elementAdded(DeviceView view, IO element) {
				viewChanged(view);
			}

			@Override
			public void elementChanged(DeviceView view, IO element) {
				viewChanged(view);
			}

			@Override
			public void elementRemoved(DeviceView view, IO element) {
				viewChanged(view);
			}
		});
	}

	@Override
	public void refresh() {
		if (Platform.isFxApplicationThread()) {
			rebuildElements();
		} else
			Platform.runLater(() -> refresh());
	}

	public final void setKeySelectionModel(MultipleSelectionModel<IO> value) {
		keySelectionModel().set(value);
	}

	@Override
	public void updateFromMatrix(int[][][] frame) {
		if (Platform.isFxApplicationThread()) {
			for (MatrixCellButton elementView : elements.get()) {
				if (elementView.getElement() instanceof MatrixIO) {
					MatrixIO matrixIO = (MatrixIO) elementView.getElement();
					int[] rgb = frame[matrixIO.getMatrixY()][matrixIO.getMatrixX()];
					if (rgb == null)
						rgb = Colors.COLOR_BLACK;
					elementView.setRgb(rgb);
				}
			}
		} else
			Platform.runLater(() -> updateFromMatrix(frame));
	}

	protected void addToSelection(IO element) {
		keySelectionModel().get().select(items.indexOf(element));
	}

	protected void clearSelection() {
		keySelectionModel().get().clearSelection();
	}

	protected void clearSelection(IO element) {
		keySelectionModel().get().clearSelection(items.indexOf(element));
	}

	protected boolean isLayoutReadOnly() {
		return view.getLayout().isReadOnly() || viewer.isReadOnly();
	}

	protected void retextLabels() {
		for (MatrixCellButton elementView : elements.get()) {
			((Label) elementView.getGraphic()).textProperty().set(elementView.getElement().getLabel());
			if (elementView.getElement().getWidth() > 0) {
				elementView.minWidthProperty().set(elementView.getElement().getWidth() * 0.30);
				elementView.prefWidthProperty().set(elementView.getElement().getWidth() * 0.30);
			} else {
				elementView.minWidthProperty().set(USE_PREF_SIZE);
			}
			if (isLayoutReadOnly())
				elementView.visibleProperty().set(!elementView.getElement().isDisabled());
			else
				elementView.setPartialDisable(elementView.getElement().isDisabled());
		}
		layout();

	}

	protected void select(IO element) {
		int idx = items.indexOf(element);
		keySelectionModel().get().clearAndSelect(idx);
	}

	protected void toggleSelection(IO element) {
		int idx = items.indexOf(element);
		if (keySelectionModel().get().isSelected(idx)) {
			keySelectionModel().get().clearSelection(idx);
		} else {
			keySelectionModel().get().select(idx);
		}

	}

	private MatrixCellButton createKeyButton(MatrixCell key) {
		MatrixCellButton but = new MatrixCellButton(this, key);
		KeyboardLayout.setCol(but, key.getMatrixX());
		KeyboardLayout.setRow(but, key.getMatrixY());
		buttons.put(but.getElement().getMatrixXY(), but);
		return but;
	}

	private void rebuildElements() {
		keyContainer.getChildren().clear();
		elements.get().clear();

		for (int i = 0; i < deviceY; i++) {
			for (int j = 0; j < deviceX; j++) {
				MatrixCellButton but;
				MatrixCell key = view.getElement(ComponentType.MATRIX_CELL, j, i);
				if (key != null) {
					but = createKeyButton(key);
					keyContainer.getChildren().add(but);
					elements.get().add(but);
				}
			}
		}
	}

	private void updateAvailability() {
	}

	public final static List<MatrixIO> expandMatrixElements(DeviceLayout layout, Collection<IO> elements) {
		List<MatrixIO> expanded = new ArrayList<>();
		DeviceView matrixView = null;
		for (IO element : elements) {
			if (element instanceof Area) {
				if (matrixView == null)
					matrixView = layout.getViews().get(ViewPosition.MATRIX);
				Area area = (Area) element;
				Region.Name region = area.getRegion();
				for (IO cell : matrixView.getElements()) {
					MatrixCell mc = (MatrixCell) cell;
					if (mc.getRegion() == region) {
						expanded.add(mc);
					}
				}
			} else if (element instanceof MatrixIO) {
				if (((MatrixIO) element).isMatrixLED())
					expanded.add((MatrixIO) element);
			}
		}
		return expanded;

	}

	public final static int[] getRGBAverage(DeviceLayout layout, Collection<IO> elements, int[][][] frame) {
		DeviceView matrixView = null;
		int[] rgb = new int[3];
		int r = 0;
		for (IO element : elements) {
			if (element instanceof Area) {
				if (matrixView == null)
					matrixView = layout.getViews().get(ViewPosition.MATRIX);
				Area area = (Area) element;
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
			} else if (element instanceof MatrixIO) {
				MatrixIO matrixIO = (MatrixIO) element;
				if (matrixIO.isMatrixLED()) {
					int[] rgbe = frame[matrixIO.getMatrixY()][matrixIO.getMatrixX()];
					if (rgb != null) {
						rgb[0] += rgbe[0];
						rgb[1] += rgbe[1];
						rgb[2] += rgbe[2];
						r++;
					}
				}
			}
		}
		if (r == 0)
			return Colors.COLOR_BLACK;
		else
			return new int[] { rgb[0] / r, rgb[1] / r, rgb[2] / r };
	}

	@Override
	public DeviceView getView() {
		return view;
	}

	@Override
	public void activeMapChanged(ProfileMap map) {
	}

	@Override
	public void profileAdded(Profile profile) {
	}

	@Override
	public void profileRemoved(Profile profile) {
	}

	@Override
	public void mapAdded(ProfileMap profile) {
	}

	@Override
	public void mapChanged(ProfileMap profile) {
	}

	@Override
	public void mapRemoved(ProfileMap profile) {
	}

}
