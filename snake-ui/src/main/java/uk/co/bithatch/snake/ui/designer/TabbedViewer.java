package uk.co.bithatch.snake.ui.designer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableListBase;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.DeviceLayout;
import uk.co.bithatch.snake.lib.layouts.DeviceView;
import uk.co.bithatch.snake.lib.layouts.IO;
import uk.co.bithatch.snake.lib.layouts.ViewPosition;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.Confirm;
import uk.co.bithatch.snake.ui.ListMultipleSelectionModel;
import uk.co.bithatch.snake.ui.util.BasicList;
import uk.co.bithatch.snake.widgets.Direction;

public class TabbedViewer extends TabPane implements Viewer {

	final static ResourceBundle bundle = ResourceBundle.getBundle(TabbedViewer.class.getName());

	private List<DeviceView> views = new ArrayList<>();
	private List<ViewerView> viewerViews = new ArrayList<>();
	private Map<Tab, ViewerView> tabMap = new HashMap<>();
	private List<ViewPosition> exclude = new ArrayList<>();
	private List<ViewPosition> include = new ArrayList<>();
	private ObjectProperty<List<ComponentType>> enabledTypes = new SimpleObjectProperty<>(this, "enabledTypes");

	private App context;
	private Device device;
	private boolean needRefresh;
	private boolean adjusting;
	private List<ViewerListener> listeners = new ArrayList<>();

	private SimpleBooleanProperty readOnly = new SimpleBooleanProperty();
	private SimpleBooleanProperty selectableElements = new SimpleBooleanProperty(true);

	public TabbedViewer(App context, Device device) {
		super();
		this.context = context;
		this.device = device;

		enabledTypes.set(new BasicList<>());
		Set<ComponentType> types = device.getSupportedComponentTypes();
		if(context.getMacroManager().isSupported(device)) {
			types.add(ComponentType.KEY);
		}
		setEnabledTypes(new ArrayList<>(types));
		setKeySelectionModel(new ListMultipleSelectionModel<>(new ObservableListBase<>() {

			@Override
			public IO get(int index) {
				return getSelectedView().getElements().get(index);
			}

			@Override
			public int size() {
				return getSelectedView().getElements().size();
			}
		}));

		getSelectionModel().selectedIndexProperty().addListener((e, o, n) -> {
			/*
			 * When tab changes, clear OUR current key selection and transfer from the newly
			 * selected view
			 */

			MultipleSelectionModel<IO> newKsv = viewerViews.get(n.intValue()).getElementSelectionModel();
			MultipleSelectionModel<IO> parentKsv = getKeySelectionModel();

			parentKsv.clearSelection();

			ViewerView selectedViewerView = getSelectedViewerView();
			for (int idx : newKsv.getSelectedIndices()) {
				parentKsv.select(selectedViewerView.getElements().get(idx));
			}

			for (int i = listeners.size() - 1; i >= 0; i--)
				listeners.get(i).viewerSelected(selectedViewerView);

			refresh();
		});

		tabDragPolicyProperty().set(TabDragPolicy.REORDER);
		getTabs().addListener(new ListChangeListener<>() {

			@Override
			public void onChanged(Change<? extends Tab> c) {
				if (!adjusting) {
					List<DeviceView> newViews = new ArrayList<>();
					for (Tab tab : getTabs()) {
						ViewerView view = tabMap.get(tab);
						newViews.add(view.getView());
					}

					/* TODO: make this better. What if views are being hidden? */
					TabbedViewer.this.views.clear();
					TabbedViewer.this.views.addAll(newViews);
					layout.setViews(newViews);
				}
			}
		});
	}

	public void addListener(ViewerListener listener) {
		this.listeners.add(listener);
	}

	public void removeListener(ViewerListener listener) {
		this.listeners.remove(listener);
	}

	@Override
	public List<ComponentType> getEnabledTypes() {
		return enabledTypes.get();
	}

	@Override
	public void setEnabledTypes(List<ComponentType> enabledTypes) {
		this.enabledTypes.set(enabledTypes);
	}

	public ObjectProperty<List<ComponentType>> enabledTypes() {
		return enabledTypes;
	}

	public SimpleBooleanProperty readOnly() {
		return readOnly;
	}

	public SimpleBooleanProperty selectableElements() {
		return selectableElements;
	}

	@Override
	protected void layoutChildren() {
		super.layoutChildren();
		if (needRefresh) {
			/*
			 * NOTE: Workaround. Because the LayoutEditor added to this component is a tab,
			 * and at the time of adding it, the parent hierarchy is not fully known (tab
			 * not visible), the first render is wrong as font metrics and other styling
			 * cannot be determined.
			 * 
			 * To fix this we do a full layout once the first time this is called after that
			 * tab has been added
			 */
			needRefresh = false;
			refresh();
		}
	}

	public final void setKeySelectionModel(MultipleSelectionModel<IO> value) {
		keySelectionModel().set(value);
	}

	public List<ViewPosition> getExclude() {
		return exclude;
	}

	public List<ViewPosition> getInclude() {
		return include;
	}

	private ObjectProperty<MultipleSelectionModel<IO>> keySelectionModel = new SimpleObjectProperty<MultipleSelectionModel<IO>>(
			this, "keySelectionModel");

	private DeviceLayout layout;

	public void setSelectionMode(SelectionMode selectionMode) {
		getKeySelectionModel().setSelectionMode(selectionMode);
		for (ViewerView viewerView : viewerViews) {
			viewerView.getElementSelectionModel().setSelectionMode(selectionMode);
		}
	}

	public IO getSelectedElement() {
		int selIdx = getSelectedViewerView().getElementSelectionModel().getSelectedIndex();
		return selIdx == -1 ? null : getSelectedViewerView().getElements().get(selIdx);
	}

	public List<IO> getSelectedElements() {
		List<IO> sel = new ArrayList<>();
		List<IO> elements = getSelectedViewerView().getElements();
		for (int idx : getSelectedViewerView().getElementSelectionModel().getSelectedIndices()) {
			sel.add(elements.get(idx));
		}
		return sel;
	}

	public final void setSelectionModel(MultipleSelectionModel<IO> value) {
		keySelectionModel().set(value);
	}

	public final MultipleSelectionModel<IO> getKeySelectionModel() {
		return keySelectionModel == null ? null : keySelectionModel.get();
	}

	public final ObjectProperty<MultipleSelectionModel<IO>> keySelectionModel() {
		return keySelectionModel;
	}

	public void setLayout(DeviceLayout layout) {
		this.layout = layout;
		getTabs().clear();
		if (this.layout != null) {
			for (DeviceView view : layout.getViews().values()) {
				addView(view);
			}
		}
	}

	public void addView(DeviceView view) {
		boolean inc = include.isEmpty() || include.contains(view.getPosition());
		if (inc)
			inc = exclude.isEmpty() || !exclude.contains(view.getPosition());
		if (inc) {
			ViewerView viewerView = createViewerView(view);
			viewerView.getElementSelectionModel().selectionModeProperty().set(keySelectionModel.get().getSelectionMode());
			addTab(viewerView, view);
			onAddView(view, viewerView);
		}
	}

	public void refresh() {
		for (ViewerView view : viewerViews) {
			view.refresh();
		}
	}

	public boolean isLayoutReadOnly() {
		return layout.isReadOnly() || isReadOnly();
	}

	public void selectView(DeviceView view) {
		int idx = views.indexOf(view);
		if (idx != -1) {
			getSelectionModel().select(idx);
		}
	}

	public void removeView(DeviceView view) {
		int idx = views.indexOf(view);
		if (idx != -1) {
			views.remove(idx);
			getTabs().remove(idx);
		}
	}

	ViewerView createViewerView(DeviceView view) {
		switch (view.getPosition()) {
		case MATRIX:
			return new MatrixView();
		default:
			return new LayoutEditor(context);
		}
	}

	void addTab(ViewerView viewerView, DeviceView view) {
		adjusting = true;
		try {
			views.add(view);
			viewerViews.add(viewerView);
			Tab tab = new Tab(bundle.getString("viewPosition." + view.getPosition().name()));
			tab.setContent(viewerView.getRoot());
			tab.setClosable(false);
			tabMap.put(tab, viewerView);
			needRefresh = true;
			viewerView.open(device, view, this);
			view.addListener((e) -> {
				tab.setText(bundle.getString("viewPosition." + view.getPosition().name()));
			});

			MultipleSelectionModel<IO> ksv = viewerView.getElementSelectionModel();
			MultipleSelectionModel<IO> parentKsv = getKeySelectionModel();
			ksv.selectedIndexProperty().addListener((e, oldVal, newVal) -> {
				if (ksv.getSelectedIndex() == -1) {
					parentKsv.clearSelection();
				} else {
					List<Integer> sel = new ArrayList<>(ksv.getSelectedIndices());
					for (Integer idx : sel) {
						IO io = viewerView.getElements().get(idx);
						parentKsv.select(io);
					}
				}
			});

			getTabs().add(tab);
		} finally {
			adjusting = false;
		}
	}

	public void deselectAll() {
		getSelectedViewerView().getElementSelectionModel().clearSelection();
	}

	public void selectAll() {
		getSelectedViewerView().getElementSelectionModel().selectAll();
	}

	public ViewerView getSelectedViewerView() {
		int idx = selectionModelProperty().get().getSelectedIndex();
		return idx == -1 ? null : viewerViews.get(idx);
	}

	public DeviceView getSelectedView() {
		int idx = selectionModelProperty().get().getSelectedIndex();
		return idx == -1 ? null : views.get(idx);
	}

	public DeviceLayout getLayout() {
		return layout;
	}

	public List<DeviceView> getViews() {
		return views;
	}

	public void cleanUp() {
		for (ViewerView view : viewerViews) {
			try {
				view.close();
			} catch (Exception e) {
			}
		}
	}

	public void updateFromMatrix(int[][][] frame) {
		for (ViewerView view : viewerViews) {
			try {
				view.updateFromMatrix(frame);
			} catch (Exception e) {
			}
		}
	}

	public void removeSelectedElements() {

		List<IO> els = getSelectedElements();
		if (els.size() == 1)
			getSelectedView().removeElement(getSelectedElement());
		else if (!els.isEmpty()) {
			Confirm confirm = context.push(Confirm.class, Direction.FADE);
			confirm.confirm(bundle, "removeMultipleElements", () -> {
				for (int i = els.size() - 1; i >= 0; i--)
					getSelectedView().removeElement(els.get(i));
			}, els.size());
		}

	}

	protected void updateTabOrder() {
	}

	protected void onAddView(DeviceView view, ViewerView viewerView) {
	}

}
