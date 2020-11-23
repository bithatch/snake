package uk.co.bithatch.snake.ui;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Duration;
import uk.co.bithatch.snake.lib.BrandingImage;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.layouts.Area;
import uk.co.bithatch.snake.lib.layouts.Cell;
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.DeviceLayout;
import uk.co.bithatch.snake.lib.layouts.DeviceLayouts;
import uk.co.bithatch.snake.lib.layouts.DeviceLayouts.Listener;
import uk.co.bithatch.snake.lib.layouts.DeviceView;
import uk.co.bithatch.snake.lib.layouts.IO;
import uk.co.bithatch.snake.lib.layouts.Key;
import uk.co.bithatch.snake.lib.layouts.MatrixCell;
import uk.co.bithatch.snake.lib.layouts.MatrixIO;
import uk.co.bithatch.snake.lib.layouts.RegionIO;
import uk.co.bithatch.snake.lib.layouts.ViewPosition;
import uk.co.bithatch.snake.ui.addons.Layout;
import uk.co.bithatch.snake.ui.designer.LayoutEditor;
import uk.co.bithatch.snake.ui.designer.TabbedViewer;
import uk.co.bithatch.snake.ui.designer.Viewer.ViewerListener;
import uk.co.bithatch.snake.ui.designer.ViewerView;
import uk.co.bithatch.snake.ui.effects.BlinkEffectHandler;
import uk.co.bithatch.snake.ui.effects.EffectAcquisition;
import uk.co.bithatch.snake.ui.util.JavaFX;
import uk.co.bithatch.snake.ui.util.Strings;
import uk.co.bithatch.snake.ui.util.Time.Timer;
import uk.co.bithatch.snake.ui.util.Visitor;
import uk.co.bithatch.snake.ui.widgets.Direction;

public class LayoutDesigner extends AbstractDetailsController implements Listener, ViewerListener {

	final static ResourceBundle bundle = ResourceBundle.getBundle(LayoutDesigner.class.getName());

	private static final String PREF_LAST_IMAGE_BROWSE_LOCATION = "lastImageBrowseLocation";

	private EffectAcquisition acq;

	@FXML
	private Button addView;
	@FXML
	private RadioButton autoImage;
	@FXML
	private Hyperlink browseImage;
	@FXML
	private Label componentType;
	@FXML
	private CheckBox desaturate;
	@FXML
	private CheckBox enabled;
	@FXML
	private Button export;
	@FXML
	private RadioButton fileImage;
	@FXML
	private Slider imageScale;
	@FXML
	private ToggleGroup imageSource;
	@FXML
	private TextField imageUri;
	@FXML
	private TextArea label;
	@FXML
	private ComboBox<Integer> matrixX;
	@FXML
	private Label matrixXLabel;
	@FXML
	private ComboBox<Integer> matrixY;
	@FXML
	private Label matrixYLabel;
	@FXML
	private Slider opacity;
	@FXML
	private ComboBox<ViewPosition> position;
	@FXML
	private Label positionLabel;
	@FXML
	private Label regionLabel;
	@FXML
	private ComboBox<Region.Name> region;
	@FXML
	private Hyperlink removeElement;
	@FXML
	private Button removeView;
	@FXML
	private TabPane sideBar;

	@FXML
	private StackPane stack;
	@FXML
	private ToolBar toolBar;
	@FXML
	private RadioButton urlImage;
	@FXML
	private Spinner<Integer> width;
	@FXML
	private Label widthLabel;

	private DeviceLayout layout;
	private boolean adjusting;
	private Timer backgroundChangeTimer = new Timer(Duration.millis(750), (ae) -> {
		changeBackgroundImage();
		updateBackgroundImageComponents();
	});
	private BlinkEffectHandler blink;
	private TabbedViewer deviceViewer;

	@Override
	public void layoutAdded(DeviceLayout layout) {
		Platform.runLater(() -> {
			if (layout.getName().equals(getDevice().getName())) {
				this.layout = layout;
				deviceViewer.setLayout(layout);
				deviceViewer.refresh();
				updateTemplateStatus();
			}
		});
	}

	@Override
	public void layoutChanged(DeviceLayout layout) {
		Platform.runLater(() -> {
			deviceViewer.refresh();
		});
	}

	@Override
	public void layoutRemoved(DeviceLayout layout) {

		Platform.runLater(() -> {
			if (deviceViewer.getLayout().equals(layout)) {
				deviceViewer.setLayout(null);
				LayoutDesigner.this.layout = null;
				deviceViewer.refresh();
				updateTemplateStatus();
			}
		});
	}

	@Override
	public void viewAdded(DeviceLayout layout, DeviceView view) {
		viewChanged(layout, view);
	}

	@Override
	public void viewChanged(DeviceLayout layout, DeviceView view) {
		if (Platform.isFxApplicationThread())
			configureForView(deviceViewer.getSelectedView());
		else
			Platform.runLater(() -> viewAdded(layout, view));
	}

	@Override
	public void viewRemoved(DeviceLayout layout, DeviceView view) {
		viewChanged(layout, view);
	}

	@Override
	protected void onDeviceCleanUp() {
		context.getLayouts().removeListener(this);
		deviceViewer.removeListener(this);
		deviceViewer.cleanUp();
		if (acq != null) {
			try {
				acq.close();
			} catch (Exception e) {
			}
		}
	}

	@Override
	protected void onSetDeviceDetails() throws Exception {
		JavaFX.bindManagedToVisible(enabled, width, widthLabel, toolBar, sideBar, removeElement, matrixX, matrixY,
				matrixXLabel, matrixYLabel, position, positionLabel, region, regionLabel);
		sideBar.visibleProperty().bind(toolBar.visibleProperty());
		matrixXLabel.labelForProperty().set(matrixX);
		matrixYLabel.labelForProperty().set(matrixY);
		matrixXLabel.labelForProperty().set(matrixX);
		matrixXLabel.visibleProperty().bind(matrixX.visibleProperty());
		matrixYLabel.visibleProperty().bind(matrixY.visibleProperty());
		widthLabel.visibleProperty().bind(width.visibleProperty());
		positionLabel.labelForProperty().set(position);
		positionLabel.visibleProperty().bind(position.visibleProperty());
		regionLabel.visibleProperty().bind(region.visibleProperty());

		deviceViewer = new TabbedViewer(context, getDevice());
		stack.getChildren().add(deviceViewer);
		DeviceLayoutManager layouts = context.getLayouts();
		deviceViewer.setSelectionMode(SelectionMode.MULTIPLE);
		boolean hasLayout = layouts.hasLayout(getDevice());
		if (!hasLayout) {
			layout = new DeviceLayout(getDevice());
			DeviceView view = new DeviceView();
			view.setPosition(ViewPosition.TOP);
			layout.addView(view);
			layouts.addLayout(layout);
		} else {
			layout = layouts.getLayout(getDevice());
		}

		if (layout.getViews().get(ViewPosition.MATRIX) == null
				&& getDevice().getCapabilities().contains(Capability.MATRIX)) {
			layout.addView(DeviceLayouts.createMatrixView(getDevice()));
		}

		width.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1024, 0, 10));
		deviceViewer.setLayout(layout);
		configureForView(deviceViewer.getSelectedView());
		updateTemplateStatus();

		region.itemsProperty().get().addAll(getDevice().getRegionNames());

		/* Setup listeners */
		enabled.selectedProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				((Key) deviceViewer.getSelectedElement()).setDisabled(!n);
			}
		});
		width.valueProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				((Key) deviceViewer.getSelectedElement()).setWidth(n);
			}
		});
		imageSource.selectedToggleProperty().addListener((e, o, n) -> {
			if (!adjusting)
				backgroundChangeTimer.reset();
		});
		imageUri.textProperty().addListener((e, o, n) -> {
			if (!adjusting)
				backgroundChangeTimer.reset();
		});
		imageScale.valueProperty().addListener((e, o, n) -> {
			if (!adjusting)
				deviceViewer.getSelectedView().setImageScale((float) (imageScale.valueProperty().get() / 100.0));
		});
		opacity.valueProperty().addListener((e, o, n) -> {
			if (!adjusting)
				deviceViewer.getSelectedView().setImageOpacity((float) (opacity.valueProperty().get() / 100.0));
		});
		desaturate.selectedProperty().addListener((e, o, n) -> {
			if (!adjusting)
				deviceViewer.getSelectedView().setDesaturateImage(desaturate.isSelected());
		});
		position.getSelectionModel().selectedItemProperty().addListener((e, oldVal, newVal) -> {
			if (!adjusting) {
				deviceViewer.getSelectedView().setPosition(position.getSelectionModel().getSelectedItem());
				backgroundChangeTimer.reset();
			}
		});
		enabled.selectedProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				for (IO el : deviceViewer.getSelectedElements()) {
					((Key) el).setDisabled(!n);
				}
			}
		});
		width.valueProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				if (o == 0 && n != 0) {
					width.getValueFactory().setValue(178);
				} else {
					for (IO el : deviceViewer.getSelectedElements()) {
						((Key) el).setWidth(n);
					}
				}
			}
		});
		label.textProperty().addListener((e) -> {
			if (!adjusting) {
				IO el = deviceViewer.getSelectedElement();
				if (el != null) {
					el.setLabel(label.textProperty().get());
				}

				/*
				 * Check the label for all elements on other views that are at the same matrix
				 * position
				 */
				if (el instanceof MatrixIO) {
					visitRelatedCells((MatrixIO) el, (other) -> other.setLabel(el.getLabel()));
				}
			}
		});
		matrixX.getSelectionModel().selectedItemProperty().addListener((e, oldVal, newVal) -> {
			if (!adjusting)
				((MatrixIO) deviceViewer.getSelectedElement())
						.setMatrixX(matrixX.getSelectionModel().getSelectedItem());
		});
		matrixY.getSelectionModel().selectedItemProperty().addListener((e, oldVal, newVal) -> {
			if (!adjusting)
				((MatrixIO) deviceViewer.getSelectedElement())
						.setMatrixY(matrixY.getSelectionModel().getSelectedItem());
		});
		deviceViewer.getSelectionModel().selectedIndexProperty().addListener((c, o, n) -> updateAvailability());
		deviceViewer.getKeySelectionModel().selectedIndexProperty().addListener((e, oldVal, newVal) -> {
			configureForView(deviceViewer.getSelectedView());
		});
		region.getSelectionModel().selectedItemProperty().addListener((e, oldVal, newVal) -> {
			if (!adjusting) {
				List<IO> selectedElements = deviceViewer.getSelectedElements();
				if (!selectedElements.isEmpty()) {
					IO selectedElement = deviceViewer.getSelectedElement();
					if (selectedElements.size() == 1 && selectedElement instanceof Area) {
						/*
						 * If we are currently on the AREA view, and the current label appears to be the
						 * old region name, the update the label to the new region name
						 */
						String oldLabel = LayoutEditor.getBestRegionName(deviceViewer.getSelectedView(), oldVal);
						if (Objects.equals(oldLabel, selectedElement.getLabel())) {
							String newLabel = LayoutEditor.getBestRegionName(deviceViewer.getSelectedView(), newVal);
							label.textProperty().set(newLabel);
							selectedElement.setLabel(newLabel);
						}
					}

					for (IO el : selectedElements) {
						if (el instanceof RegionIO) {
							/* For element types that actually have a region, set it directly */
							((RegionIO) el).setRegion(newVal);
						} else if (el instanceof MatrixIO) {
							/*
							 * For element types that are associated with matrix cells, set the region on
							 * that matrix cell element it is associated with.
							 */
							MatrixIO m = (MatrixIO) el;
							MatrixCell cell = (MatrixCell) layout.getViews().get(ViewPosition.MATRIX)
									.getElement(ComponentType.MATRIX_CELL, m.getMatrixX(), m.getMatrixY());
							if (cell != null) {
								cell.setRegion(newVal);
							}
						}
					}
				}
			}

		});
		context.getLayouts().addListener(this);
		deviceViewer.addListener(this);

		/*
		 * If this is a Matrix device, acquire effects control on the device. This will
		 * shutdown the current effect. We then highlight individual LED lights as they
		 * are selected
		 */
		if (getDevice().getCapabilities().contains(Capability.MATRIX)) {
			acq = context.getEffectManager().acquire(getDevice());
			blink = new BlinkEffectHandler() {
				@Override
				public void update(Lit component) {
					super.update(component);
					deviceViewer.updateFromMatrix(getEffect().getCells());
				}
			};
			blink.open(context, getDevice());
			acq.activate(getDevice(), blink);
		}
	}

	void visitRelatedCells(MatrixIO mio, Visitor<MatrixIO> visit) {
		for (Map.Entry<ViewPosition, DeviceView> en : deviceViewer.getLayout().getViews().entrySet()) {
			if (en.getValue() != deviceViewer.getSelectedView()) {
				for (IO otherEl : en.getValue().getElements()) {
					if (otherEl instanceof MatrixIO) {
						MatrixIO otherMio = (MatrixIO) otherEl;
						if (mio.getMatrixX() == otherMio.getMatrixX() && mio.getMatrixY() == otherMio.getMatrixY()) {
							visit.visit(otherMio);
						}
					}
				}
			}
		}
	}

	void addView(ViewPosition viewPosition) {
		DeviceView view = new DeviceView();
		view.setPosition(viewPosition);
		deviceViewer.getLayout().addView(view);
		deviceViewer.addView(view);
		deviceViewer.selectView(view);
		updateAvailability();
	}

	void changeBackgroundImage() {
		String uri = imageUri.textProperty().get();
		if (autoImage.selectedProperty().get())
			uri = null;
		else {
			if (uri == null || uri.equals("")) {
				BrandingImage bimg = deviceViewer.getSelectedView().getPosition().toBrandingImage();
				if (bimg != null) {
					if (fileImage.selectedProperty().get())
						uri = context.getCache().getCachedImage(getDevice().getImageUrl(bimg));
					else
						uri = getDevice().getImageUrl(bimg);
				}
			} else {
				URL url = null;
				try {
					url = new URL(uri);
				} catch (Exception e) {
				}
				if (url != null) {
					if (fileImage.selectedProperty().get()
							&& (url.getProtocol().equals("http") || url.getProtocol().equals("https"))) {
						imageUri.textProperty().set("");
						uri = null;
					} else if (urlImage.selectedProperty().get() && url.getProtocol().equals("file")) {
						imageUri.textProperty().set("");
						uri = null;
					}
				}
			}
		}
		imageUri.setTooltip(new Tooltip(uri));
		deviceViewer.getSelectedView().setImageUri(uri);
		deviceViewer.refresh();
	}

	void configureForView(DeviceView view) {
		adjusting = true;
		try {
			matrixX.itemsProperty().get().clear();
			matrixY.itemsProperty().get().clear();
			position.itemsProperty().get().clear();
			if (view != null) {

				DeviceLayout deviceLayout = view.getLayout();
				for (int i = 0; i < deviceLayout.getMatrixHeight(); i++)
					matrixY.itemsProperty().get().add(i);

				for (int i = 0; i < deviceLayout.getMatrixWidth(); i++)
					matrixX.itemsProperty().get().add(i);

				ViewPosition selectedPosition = view.getPosition();
				position.itemsProperty().get().add(selectedPosition);

//				XX
//				XX Here! Got a progblem when removing a view.addElement(The POSITION list doesnt change);
//				XX 
				for (ViewPosition viewPosition : ViewPosition.values()) {
					if (viewPosition != ViewPosition.MATRIX && viewPosition != selectedPosition
							&& !deviceLayout.getViews().containsKey(viewPosition)) {
						position.itemsProperty().get().add(viewPosition);
					}
				}

				position.selectionModelProperty().get().select(selectedPosition);
				imageScale.valueProperty().set(view.getImageScale() * 100.0);
				opacity.valueProperty().set(view.getImageOpacity() * 100.0);
				desaturate.selectedProperty().set(view.isDesaturateImage());

				List<IO> elements = deviceViewer.getSelectedElements();
				boolean viewingMatrix = deviceViewer.getSelectedView().getPosition().equals(ViewPosition.MATRIX);

				int areMatrixIO = 0;
				for (IO io : elements) {
					if (io instanceof MatrixIO)
						areMatrixIO++;
				}

				matrixX.setVisible(
						matrixX.itemsProperty().get().size() > 1 && areMatrixIO == elements.size() && areMatrixIO > 0);
				matrixY.setVisible(
						matrixY.itemsProperty().get().size() > 1 && areMatrixIO == elements.size() && areMatrixIO > 0);
				enabled.setVisible(viewingMatrix);
				width.setVisible(viewingMatrix);
				position.setVisible(!viewingMatrix);

				if (elements.size() == 1) {
					IO element = elements.get(0);
					if (!Objects.equals(element.getLabel(), label.textProperty().get()))
						label.textProperty().set(element.getLabel());
					componentType.textProperty()
							.set(bundle.getString("componentType." + ComponentType.fromClass(element.getClass())));
					region.setDisable(false);
					region.setVisible(
							element instanceof Area || getDevice().getCapabilities().contains(Capability.MATRIX));
					removeElement.setVisible(false);

					/*
					 * When dealing with an area or a matrix cell, the region is as specified in the
					 * element itself.
					 */
					if (element instanceof RegionIO) {
						RegionIO r = (RegionIO) element;
						region.getSelectionModel().select(r.getRegion());
					}

					if (element instanceof MatrixIO && getDevice().getCapabilities().contains(Capability.MATRIX)) {
						MatrixIO m = (MatrixIO) element;

						/*
						 * For other types, the region is the region of the matrix cell the element is
						 * associated with
						 */
						MatrixCell cell = (MatrixCell) deviceLayout.getViews().get(ViewPosition.MATRIX)
								.getElement(ComponentType.MATRIX_CELL, m.getMatrixX(), m.getMatrixY());
						if (cell != null) {
							region.getSelectionModel().select(cell.getRegion());
						}

						matrixX.setDisable(viewingMatrix);
						matrixY.setDisable(viewingMatrix);
						matrixX.selectionModelProperty().get().select(m.getMatrixX());
						matrixY.selectionModelProperty().get().select(m.getMatrixY());
						if (blink != null)
							blink.highlight(getDevice(), m.getMatrixX(), m.getMatrixY());
					} else {
						matrixX.setDisable(true);
						matrixY.setDisable(true);
					}
					enabled.selectedProperty().set(!(element instanceof Key) || !((Key) element).isDisabled());
					width.getValueFactory().setValue(!(element instanceof Key) ? 0 : ((Key) element).getWidth());
					enabled.setDisable(!viewingMatrix);
					width.setDisable(!viewingMatrix);
				} else if (elements.isEmpty()) {
					if (blink != null)
						blink.clear(getDevice());
					matrixX.setDisable(true);
					matrixY.setDisable(true);
					region.setDisable(true);
					region.setVisible(getDevice().getCapabilities().contains(Capability.MATRIX));
					enabled.setDisable(true);
					width.setDisable(true);
					region.setVisible(true);
				} else {
					matrixX.setDisable(true);
					matrixY.setDisable(true);
					region.setDisable(false);
					int keys = 0;
					int areas = 0;
					for (IO el : elements) {
						if (el instanceof Key)
							keys++;
						if (el instanceof Area)
							areas++;
					}
					region.setVisible(
							areas == elements.size() || getDevice().getCapabilities().contains(Capability.MATRIX));
					enabled.setDisable(keys != elements.size());
					width.setDisable(keys != elements.size());
				}

				label.setDisable(elements.size() != 1);
				removeElement.setVisible(!elements.isEmpty() && !viewingMatrix);

				if (blink != null && elements.size() > 0) {
					Set<Cell> highlightCells = new HashSet<>();
					DeviceView matrixView = deviceLayout.getViews().get(ViewPosition.MATRIX);
					for (IO element : elements) {
						if (element instanceof MatrixIO)
							highlightCells.add(((MatrixIO) element).getMatrixXY());
						else if (element instanceof Area) {
							/* Add all of the cells in this area to highlight */
							Region.Name region = ((Area) element).getRegion();
							for (IO mel : matrixView.getElements()) {
								MatrixCell mcell = (MatrixCell) mel;
								if (Objects.equals(region, mcell.getRegion())) {
									highlightCells.add(mcell.getMatrixXY());
								}
							}
						}
					}
					blink.highlight(getDevice(), highlightCells.toArray(new Cell[0]));
				}

				updateBackgroundImageComponents();
				updateAvailability();
			}
		} finally {
			adjusting = false;
		}
	}

	@FXML
	void evtAddView() {
		/* Find the next free view slot */
		for (ViewPosition viewPosition : ViewPosition.values()) {
			if (viewPosition != ViewPosition.MATRIX && !deviceViewer.getLayout().getViews().containsKey(viewPosition)) {
				addView(viewPosition);
				return;
			}
		}
	}

	@FXML
	void evtBrowseImage() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("browseImage.selectImageFile"));
		var path = Strings.defaultIfBlank(imageUri.textProperty().get(), getDefaultOutputLocation());
		fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("browseImage.imageFileExtensions"),
				"*.png", "*.jpeg", "*.jpg", "*.gif"));
		fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("browseImage.allFiles"), "*.*"));
		JavaFX.selectFilesDir(fileChooser, path);
		File file = fileChooser.showOpenDialog(getScene().getWindow());
		if (file != null) {
			context.getPreferences(getDevice().getName()).put(PREF_LAST_IMAGE_BROWSE_LOCATION, file.getAbsolutePath());
			imageUri.textProperty().set(file.getAbsolutePath());
		}
	}

	@FXML
	void evtExport() {
		Layout addOn = new Layout(Strings.toId(getDevice().getName()));
		DeviceLayout sequence = deviceViewer.getLayout();
		addOn.setName(getDevice().getName());
		addOn.setDescription(MessageFormat.format(bundle.getString("addOnTemplate.description"), getDevice().getName(),
				getDevice().getDriverVersion(), getDevice().getFirmware(), context.getBackend().getName(),
				context.getBackend().getVersion(), PlatformService.get().getInstalledVersion()));
		addOn.setLayout(sequence);
		Export confirm = context.push(Export.class, Direction.FADE);
		confirm.export(addOn, bundle, "exportLayout", getDevice().getName());
	}

	@FXML
	void evtRemoveElement() {
		deviceViewer.removeSelectedElements();
	}

	@FXML
	void evtRemoveView() {
		Confirm confirm = context.push(Confirm.class, Direction.FADE);
		confirm.confirm(bundle, "removeView", () -> {
			DeviceView view = deviceViewer.getSelectedView();
			deviceViewer.getLayout().removeView(view.getPosition());
			deviceViewer.removeView(view);
			updateAvailability();
		}, deviceViewer.getSelectedView().getPosition());
	}

	String getDefaultOutputLocation() {
		return context.getPreferences(getDevice().getName()).get(PREF_LAST_IMAGE_BROWSE_LOCATION,
				System.getProperty("user.dir"));
	}

	void updateAvailability() {
		removeView.setDisable(deviceViewer.getViews().size() < 2
				|| deviceViewer.getSelectedView().getPosition().equals(ViewPosition.MATRIX));
	}

	void updateBackgroundImageComponents() {
		/* Is the background image a complete URL? */
		boolean isURL = false;
		URL url = null;
		DeviceView selectedView = deviceViewer.getSelectedView();
		try {
			url = new URL(selectedView.getImageUri());
			isURL = true;
		} catch (Exception e) {
		}

		/* Is the background a file URL, so local */
		boolean isFile = false;
		if (url != null && url.getProtocol().equals("file")) {
			isFile = true;
		}

		if (url == null) {
			if (fileImage.selectedProperty().get())
				isFile = true;
			else if (urlImage.selectedProperty().get())
				isFile = true;
		}

		if (url != null) {
			if (isFile) {
				try {
					imageUri.promptTextProperty().set(new File(url.toURI()).getAbsolutePath());
				} catch (URISyntaxException e) {
					imageUri.promptTextProperty().set(url.toExternalForm());
				}
				fileImage.selectedProperty().set(true);
			} else if (isURL) {
				imageUri.promptTextProperty().set(url.toExternalForm());
				urlImage.selectedProperty().set(true);
			} else {
				BrandingImage bimg = selectedView.getPosition().toBrandingImage();
				if (bimg != null) {
					imageUri.promptTextProperty().set(getDevice().getImageUrl(bimg));
				}
				autoImage.selectedProperty().set(true);
			}
		}
		imageUri.setDisable(!isFile && !isURL);
		browseImage.setDisable(!isFile);
	}

	void updateTemplateStatus() {
		toolBar.setVisible(layout != null && !layout.isReadOnly());
		DeviceLayoutManager layouts = context.getLayouts();
		boolean hasContributedLayout = layouts.hasLayout(getDevice()) && layouts.getLayout(getDevice()).isReadOnly();
		boolean hasOfficialLayout = context.getLayouts().hasOfficialLayout(getDevice());
		if (!hasOfficialLayout) {
			if (hasContributedLayout) {
				notifyMessage(MessagePersistence.ONCE_PER_RUNTIME, MessageType.INFO,
						bundle.getString("info.contributed"));
			} else
				notifyMessage(MessagePersistence.ONCE_PER_RUNTIME, MessageType.INFO,
						bundle.getString("info.notOfficial"));
		} else {
			clearNotifications(false);
		}
	}

	@Override
	public void viewerSelected(ViewerView view) {
		configureForView(view.getView());
	}
}
