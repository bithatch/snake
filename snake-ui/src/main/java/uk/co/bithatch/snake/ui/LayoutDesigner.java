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

import org.controlsfx.control.SearchableComboBox;
import org.controlsfx.control.ToggleSwitch;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
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
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.util.Callback;
import javafx.util.Duration;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.snake.lib.BrandingImage;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.effects.Off;
import uk.co.bithatch.snake.lib.layouts.Accessory;
import uk.co.bithatch.snake.lib.layouts.Accessory.AccessoryType;
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
	private CheckBox desaturate;
	@FXML
	private ToggleSwitch enabled;
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
	private Spinner<Integer> matrixX;
	@FXML
	private Label matrixXLabel;
	@FXML
	private Spinner<Integer> matrixY;
	@FXML
	private Label matrixYLabel;
	@FXML
	private Label labelLabel;
	@FXML
	private Slider opacity;
	@FXML
	private ComboBox<ViewPosition> position;
	@FXML
	private ComboBox<AccessoryType> accessory;
	@FXML
	private Label positionLabel;
	@FXML
	private Label regionLabel;
	@FXML
	private Label keyMappingLabel;
	@FXML
	private Label legacyKeyMappingLabel;
	@FXML
	private Label accessoryLabel;
	@FXML
	private Label matrixLabel;
	@FXML
	private Label noSelection;
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
	@FXML
	private SearchableComboBox<EventCode> keyMapping;
	@FXML
	private SearchableComboBox<uk.co.bithatch.snake.lib.Key> legacyKeyMapping;

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
			configureForView();
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
			configureForView();
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
				matrixXLabel, matrixYLabel, position, positionLabel, region, regionLabel, legacyKeyMapping, legacyKeyMappingLabel, keyMapping, keyMappingLabel,
				accessory, accessoryLabel, labelLabel, label, matrixLabel, noSelection);

		matrixXLabel.labelForProperty().set(matrixX);
		matrixYLabel.labelForProperty().set(matrixY);
		positionLabel.labelForProperty().set(position);

		sideBar.visibleProperty().bind(toolBar.visibleProperty());
		matrixXLabel.visibleProperty().bind(matrixX.visibleProperty());
		matrixYLabel.visibleProperty().bind(matrixY.visibleProperty());
		widthLabel.visibleProperty().bind(width.visibleProperty());
		positionLabel.visibleProperty().bind(position.visibleProperty());
		regionLabel.visibleProperty().bind(region.visibleProperty());
		accessoryLabel.visibleProperty().bind(accessory.visibleProperty());
		labelLabel.visibleProperty().bind(label.visibleProperty());
		keyMappingLabel.visibleProperty().bind(keyMapping.visibleProperty());
		legacyKeyMappingLabel.visibleProperty().bind(legacyKeyMapping.visibleProperty());
		matrixLabel.visibleProperty().bind(Bindings.or(matrixX.visibleProperty(), Bindings
				.or(Bindings.or(keyMapping.visibleProperty(), matrixY.visibleProperty()), matrixY.visibleProperty())));

		Callback<ListView<Region.Name>, ListCell<Region.Name>> factory = new Callback<ListView<Region.Name>, ListCell<Region.Name>>() {

			@Override
			public ListCell<Region.Name> call(ListView<Region.Name> l) {
				return new ListCell<Region.Name>() {

					@Override
					protected void updateItem(Region.Name item, boolean empty) {
						super.updateItem(item, empty);
						String imageUrl;
						if (item == null || empty) {
							imageUrl = context.getConfiguration().themeProperty().getValue()
									.getEffectImage(24, Off.class).toExternalForm();
							setText(bundle.getString("noRegion"));
						} else {
							imageUrl = context.getConfiguration().themeProperty().getValue().getRegionImage(24, item)
									.toExternalForm();
							setText(bundle.getString("region." + item.name()));
						}
						ImageView iv = new ImageView(imageUrl);
						iv.setFitHeight(22);
						iv.setFitWidth(22);
						iv.setSmooth(true);
						iv.setPreserveRatio(true);
						iv.getStyleClass().add("cell");
						setGraphic(iv);
					}
				};
			}
		};
		region.setCellFactory(factory);
		region.setButtonCell(factory.call(null));

		Device device = getDevice();
		deviceViewer = new TabbedViewer(context, device);
		stack.getChildren().add(deviceViewer);
		DeviceLayoutManager layouts = context.getLayouts();
		deviceViewer.setSelectionMode(SelectionMode.MULTIPLE);
		boolean hasLayout = layouts.hasLayout(device);
		if (!hasLayout) {
			layout = new DeviceLayout(device);
			DeviceView view = new DeviceView();
			view.setPosition(ViewPosition.TOP);
			layout.addView(view);
			layouts.addLayout(layout);
		} else {
			layout = layouts.getLayout(device);
		}

		if (layout.getViews().get(ViewPosition.MATRIX) == null
				&& device.getCapabilities().contains(Capability.MATRIX)) {
			layout.addView(DeviceLayouts.createMatrixView(device));
		}

		width.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 1024, 0, 10));
		deviceViewer.setLayout(layout);
		configureForView();
		updateTemplateStatus();

		accessory.getItems().addAll(Accessory.AccessoryType.values());

		keyMapping.getItems().add(null);
		keyMapping.getItems().addAll(device.getSupportedInputEvents());
		legacyKeyMapping.getItems().add(null);
		legacyKeyMapping.getItems().addAll(device.getSupportedLegacyKeys());

		/* Setup listeners */
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
					if (el instanceof MatrixCell)
						((MatrixCell) el).setDisabled(!n);
				}
			}
		});
		width.valueProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				if (o == 0 && n != 0) {
					width.getValueFactory().setValue(178);
				} else {
					for (IO el : deviceViewer.getSelectedElements()) {
						if (el instanceof MatrixCell)
							((MatrixCell) el).setWidth(n);
					}
				}
			}
		});
		label.textProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				IO el = deviceViewer.getSelectedElement();
				if (el != null) {
					el.setLabel(n.equals("") ? null : n);
				}
			}
		});
		matrixX.valueProperty().addListener((e, oldVal, newVal) -> {
			if (!adjusting) {
				List<IO> selectedElements = deviceViewer.getSelectedElements();
				if (!selectedElements.isEmpty()) {
					Integer mx = matrixX.getValue();
					for (IO el : selectedElements) {
						if (el instanceof MatrixIO) {
							MatrixIO m = (MatrixIO) el;
							m.setMatrixX(mx);
							MatrixCell cell = (MatrixCell) layout.getViews().get(ViewPosition.MATRIX)
									.getElement(ComponentType.MATRIX_CELL, m.getMatrixX(), m.getMatrixY());
							if (cell != null) {
								cell.setMatrixX(mx);
							}
						}
					}
				}
			}
		});
		matrixY.valueProperty().addListener((e, oldVal, newVal) -> {
			if (!adjusting) {
				List<IO> selectedElements = deviceViewer.getSelectedElements();
				if (!selectedElements.isEmpty()) {
					Integer my = matrixY.getValue();
					for (IO el : selectedElements) {
						if (el instanceof MatrixIO) {
							MatrixIO m = (MatrixIO) el;
							m.setMatrixY(my);
							MatrixCell cell = (MatrixCell) layout.getViews().get(ViewPosition.MATRIX)
									.getElement(ComponentType.MATRIX_CELL, m.getMatrixX(), m.getMatrixY());
							if (cell != null) {
								cell.setMatrixY(my);
							}
						}
					}
				}
			}
		});
		keyMapping.getSelectionModel().selectedItemProperty().addListener((e, oldVal, newVal) -> {
			if (!adjusting) {
				IO selectedElement = deviceViewer.getSelectedElement();
				if (selectedElement != null) {
					if (selectedElement instanceof Key) {
						Key m = (Key) selectedElement;
						m.setEventCode(newVal);
					}
				}
			}
		});
		legacyKeyMapping.getSelectionModel().selectedItemProperty().addListener((e, oldVal, newVal) -> {
			if (!adjusting) {
				IO selectedElement = deviceViewer.getSelectedElement();
				if (selectedElement != null) {
					if (selectedElement instanceof Key) {
						Key m = (Key) selectedElement;
						m.setLegacyKey(newVal);
					}
				}
			}
		});
		deviceViewer.getSelectionModel().selectedIndexProperty().addListener((c, o, n) -> {
			configureForView();
			updateAvailability();
		});
		deviceViewer.getKeySelectionModel().selectedIndexProperty().addListener((e, oldVal, newVal) -> {
			configureForView();
		});
		region.getSelectionModel().selectedItemProperty().addListener((e, oldVal, newVal) -> {
			if (!adjusting) {
				List<IO> selectedElements = deviceViewer.getSelectedElements();
				if (!selectedElements.isEmpty()) {

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
							MatrixCell cell = m.isMatrixLED()
									? (MatrixCell) layout.getViews().get(ViewPosition.MATRIX)
											.getElement(ComponentType.MATRIX_CELL, m.getMatrixX(), m.getMatrixY())
									: null;
							if (cell != null) {
								cell.setRegion(newVal);
							} 
							if(newVal == null)
								m.setMatrixXY(null);
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
		if (device.getCapabilities().contains(Capability.MATRIX)) {
			acq = context.getEffectManager().acquire(device);
			blink = new BlinkEffectHandler() {
				@Override
				public void update(Lit component) {
					super.update(component);
					deviceViewer.updateFromMatrix(getEffect().getCells());
				}
			};
			blink.open(context, device);
			acq.activate(device, blink);
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

	void configureForView() {
		adjusting = true;
		DeviceView view = deviceViewer.getSelectedView();
		try {
			position.getItems().clear();
			if (view != null) {

				DeviceLayout deviceLayout = view.getLayout();
				matrixX.setValueFactory(
						new SpinnerValueFactory.IntegerSpinnerValueFactory(0, deviceLayout.getMatrixWidth(), 1, 1));
				matrixY.setValueFactory(
						new SpinnerValueFactory.IntegerSpinnerValueFactory(0, deviceLayout.getMatrixHeight(), 1, 1));

				ViewPosition selectedPosition = view.getPosition();
				position.getItems().add(selectedPosition);

//				XX
//				XX Here! Got a progblem when removing a view.addElement(The POSITION list doesnt change);
//				XX 
				for (ViewPosition viewPosition : ViewPosition.values()) {
					if (viewPosition != ViewPosition.MATRIX && viewPosition != selectedPosition
							&& !deviceLayout.getViews().containsKey(viewPosition)) {
						position.getItems().add(viewPosition);
					}
				}

				position.selectionModelProperty().get().select(selectedPosition);
				imageScale.valueProperty().set(view.getImageScale() * 100.0);
				opacity.valueProperty().set(view.getImageOpacity() * 100.0);
				desaturate.selectedProperty().set(view.isDesaturateImage());
				List<IO> elements = deviceViewer.getSelectedElements();
				boolean viewingMatrix = deviceViewer.getSelectedView().getPosition().equals(ViewPosition.MATRIX);

				region.getItems().clear();
				region.getItems().addAll(getDevice().getRegionNames());
				Region.Name selectedRegion = null;

				int cells = 0;
				int areas = 0;
				int matrixEls = 0;
				int noLighting = 0;
				int keys = 0;
				int accessories = 0;
				for (IO el : elements) {
					if (el instanceof MatrixIO) {
						matrixEls++;
						if (!((MatrixIO) el).isMatrixLED())
							noLighting++;
						else {
							MatrixCell cell = ((MatrixIO) el).getMatrixCell();
							if (cell != null)
								selectedRegion = cell.getRegion();
						}

						region.getSelectionModel().select(selectedRegion);
					}
					if (el instanceof MatrixCell)
						cells++;
					if (el instanceof Area)
						areas++;
					if (el instanceof Key)
						keys++;
					if (el instanceof Accessory) {
						noLighting++;
						accessories++;
					}
					if (el instanceof RegionIO && selectedRegion == null)
						selectedRegion = ((RegionIO) el).getRegion();
				}

				matrixX.setVisible(noLighting ==0 && deviceLayout.getMatrixWidth() > 1 && matrixEls == elements.size() && matrixEls > 0);
				matrixY.setVisible(noLighting ==0 && deviceLayout.getMatrixHeight() > 1 && matrixEls == elements.size() && matrixEls > 0);
				position.setVisible(!viewingMatrix);
				noSelection.textProperty().set(bundle.getString(viewingMatrix ? "noMatrixSelection" : "noSelection"));

				if (areas > 0 || keys > 0) {
					region.getItems().add(null);
				}

				if (elements.size() == 1) {
					IO element = elements.get(0);

					if (!Objects.equals(element.getLabel(), label.textProperty().get()))
						label.textProperty().set(element.getLabel());
					region.setDisable(false);
					region.setVisible(accessories == 0
							&& (areas > 0 || getDevice().getCapabilities().contains(Capability.MATRIX)));
					removeElement.setVisible(false);

					enabled.setVisible(viewingMatrix);
					width.setVisible(viewingMatrix);
					label.promptTextProperty().set(element.getDefaultLabel());

					/*
					 * When dealing with an area or a matrix cell, the region is as specified in the
					 * element itself.
					 */
					if (element instanceof RegionIO) {
						accessory.setVisible(false);
						accessory.setDisable(true);
					} else {
						/*
						 * Otherwise it is either the override region (if there is one), or the region
						 * the associated matix cell is in.
						 */
						if (element instanceof MatrixIO) {
							accessory.setVisible(false);
							accessory.setDisable(true);
							MatrixCell cell = ((MatrixIO) element).getMatrixCell();
							if (cell != null)
								selectedRegion = cell.getRegion();
						} else if (element instanceof Accessory) {
							accessory.setVisible(true);
							accessory.setDisable(false);
						}
					}

					if (element instanceof Key) {
						keyMapping.getSelectionModel().select(((Key) element).getEventCode());
						legacyKeyMapping.getSelectionModel().select(((Key) element).getLegacyKey());
					}
					else {
						keyMapping.getSelectionModel().clearSelection();
						legacyKeyMapping.getSelectionModel().clearSelection();
					}

					if (element instanceof MatrixIO && getDevice().getCapabilities().contains(Capability.MATRIX)) {
						MatrixIO m = (MatrixIO) element;
						matrixX.setDisable(viewingMatrix || !m.isMatrixLED());
						matrixY.setDisable(viewingMatrix || !m.isMatrixLED());
						matrixX.getValueFactory().setValue(m.getMatrixX());
						matrixY.getValueFactory().setValue(m.getMatrixY());
						if (blink != null)
							blink.highlight(getDevice(), m.getMatrixX(), m.getMatrixY());
					} else {
						matrixX.setDisable(true);
						matrixY.setDisable(true);
					}
					enabled.selectedProperty()
							.set(!(element instanceof MatrixCell) || !((MatrixCell) element).isDisabled());
					width.getValueFactory()
							.setValue(!(element instanceof MatrixCell) ? 0 : ((MatrixCell) element).getWidth());
					enabled.setDisable(!viewingMatrix);
					width.setDisable(!viewingMatrix);
					keyMapping.setVisible(element instanceof Key);
					legacyKeyMapping.setVisible(element instanceof Key);
					label.setVisible(true);
					noSelection.setVisible(false);
				} else if (elements.isEmpty()) {
					if (blink != null)
						blink.clear(getDevice());
					matrixX.setDisable(true);
					matrixY.setDisable(true);
					region.setDisable(true);
					region.setVisible(false);
					label.setVisible(false);
					enabled.setDisable(true);
					enabled.setVisible(false);
					label.promptTextProperty().set("");
					width.setVisible(false);
					width.setDisable(true);
					keyMapping.setVisible(false);
					legacyKeyMapping.setVisible(false);
					accessory.setVisible(false);
					accessory.setDisable(true);
					noSelection.setVisible(true);
				} else {
					label.setVisible(true);
					region.setDisable(false);
					noSelection.setVisible(false);
					accessory.setDisable(true);

					boolean noMatrix = matrixEls != elements.size()
							|| !getDevice().getCapabilities().contains(Capability.MATRIX);
					matrixX.setDisable(viewingMatrix || noMatrix || noLighting > 0);
					matrixY.setDisable(viewingMatrix || noMatrix || noLighting > 0);
					enabled.setVisible(viewingMatrix && cells == elements.size());
					label.setVisible(false);
					width.setVisible(viewingMatrix && cells == elements.size());
					region.setVisible(accessories == 0
							&& (areas == elements.size() || getDevice().getCapabilities().contains(Capability.MATRIX)));
					enabled.setDisable(cells != elements.size());
					width.setDisable(cells != elements.size());
					keyMapping.setVisible(false);
					legacyKeyMapping.setVisible(false);
					accessory.setVisible(false);
				}

				region.getSelectionModel().select(selectedRegion);
				label.setDisable(elements.size() != 1);
				removeElement.setVisible(!elements.isEmpty() && !viewingMatrix);

				if (blink != null && elements.size() > 0) {
					Set<Cell> highlightCells = new HashSet<>();
					DeviceView matrixView = deviceLayout.getViews().get(ViewPosition.MATRIX);
					for (IO element : elements) {
						if (element instanceof MatrixIO && ((MatrixIO)element).isMatrixLED())
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
		DeviceLayout layout = deviceViewer.getLayout();
		addOn.setName(getDevice().getName());
		addOn.setDescription(MessageFormat.format(bundle.getString("addOnTemplate.description"), getDevice().getName(),
				getDevice().getDriverVersion(), getDevice().getFirmware(), context.getBackend().getName(),
				context.getBackend().getVersion(), PlatformService.get().getInstalledVersion()));
		addOn.setLayout(new DeviceLayout(layout));
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
		configureForView();
	}
}
