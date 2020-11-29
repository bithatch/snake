package uk.co.bithatch.snake.ui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.Glow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.FramePlayer.FrameListener;
import uk.co.bithatch.snake.lib.KeyFrame;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.Sequence;
import uk.co.bithatch.snake.lib.layouts.Accessory;
import uk.co.bithatch.snake.lib.layouts.Accessory.AccessoryType;
import uk.co.bithatch.snake.lib.layouts.Area;
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.DeviceLayout;
import uk.co.bithatch.snake.lib.layouts.DeviceLayout.Listener;
import uk.co.bithatch.snake.lib.layouts.DeviceView;
import uk.co.bithatch.snake.lib.layouts.IO;
import uk.co.bithatch.snake.ui.designer.LayoutEditor;
import uk.co.bithatch.snake.ui.designer.Viewer;
import uk.co.bithatch.snake.ui.designer.ViewerView;
import uk.co.bithatch.snake.ui.effects.CustomEffectHandler;
import uk.co.bithatch.snake.ui.effects.EffectAcquisition;
import uk.co.bithatch.snake.ui.effects.EffectManager;
import uk.co.bithatch.snake.ui.util.BasicList;
import uk.co.bithatch.snake.ui.util.JavaFX;
import uk.co.bithatch.snake.ui.widgets.AnimPane;
import uk.co.bithatch.snake.ui.widgets.Direction;
import uk.co.bithatch.snake.ui.widgets.SlideyStack;

public class LayoutControl extends AbstractEffectsControl implements Viewer, FrameListener, Listener {

	final static ResourceBundle bundle = ResourceBundle.getBundle(LayoutControl.class.getName());

	private static final double SELECTED_GLOW_AMOUNT = 1;
	private static final int OVERALL_EFFECT_ICON_SIZE = 32;
	private static final int REGION_EFFECT_ICON_SIZE = 24;

	@FXML
	private BorderPane layout;
	@FXML
	private BorderPane top;
	@FXML
	private StackPane layoutContainer;
	@FXML
	private Hyperlink pageLeft;
	@FXML
	private Hyperlink pageRight;

	private DeviceLayout deviceLayout;
	private EffectBar overallEffect;
	private List<ViewerListener> listeners = new ArrayList<>();

	private SimpleBooleanProperty readOnly = new SimpleBooleanProperty(true);
	private SimpleBooleanProperty selectableElements = new SimpleBooleanProperty(false);
	private ObjectProperty<List<ComponentType>> enabledTypes = new SimpleObjectProperty<>(this, "enabledTypes");
	private Map<Region.Name, Node> regions = new HashMap<>();
	private AnimPane stack;
	private CustomEffectHandler currentEffect;
	private List<DeviceView> views = new ArrayList<>();

	private VBox legacyProfileAccessory;

	private ProfileControl profileAccessory;

	@Override
	protected void onSetEffectsControlDevice() {
		enabledTypes.set(new BasicList<>());
		enabledTypes.get().add(ComponentType.AREA);
		enabledTypes.get().add(ComponentType.ACCESSORY);
		stack.setContent(Direction.FADE, createEditor(views.get(0)));
		layoutContainer.getChildren().add(stack);
		overallEffect.effect.addListener((e, o, n) -> {
			if (!adjustingOverall) {
				setCustomiseState(customise, getDevice(), getOverallEffect());
				rebuildRegions();
			}
		});
		checkForCustomEffect();
		rebuildRegions();

	}

	protected LayoutEditor createEditor(DeviceView view) {
		LayoutEditor layoutEditor = new LayoutEditor(context);
		layoutEditor.setShowElementGraphics(false);
		layoutEditor.setSelectableComponentType(false);
		layoutEditor.setComponentType(ComponentType.AREA);
		layoutEditor.setLabelFactory((el) -> {
			if (el instanceof Area) {
				Area area = (Area) el;
				Region.Name regionName = area.getRegion();
				return regions.get(regionName);
			} else if (el instanceof Accessory) {
				Accessory accessory = (Accessory) el;
				if (accessory.getAccessory() == AccessoryType.PROFILES
						&& getDevice().getCapabilities().contains(Capability.MACROS)) {
					if (getDevice().getCapabilities().contains(Capability.MACRO_PROFILES))
						return createProfileAccessory(accessory);
					else
						return createLegacyProfileAccessory(accessory);
				}
			}
			return null;
		});
		layoutEditor.open(getDevice(), view, this);
		return layoutEditor;

	}

	protected Node createProfileAccessory(Accessory accessory) {
		if (profileAccessory == null) {
			try {
				profileAccessory = context.openScene(ProfileControl.class);
			} catch (IOException e) {
				throw new IllegalStateException("Failed to load control.", e);
			}
			profileAccessory.setDevice(getDevice());
		}
		return profileAccessory.getScene().getRoot();
	}

	protected VBox createLegacyProfileAccessory(Accessory accessory) {
		if (legacyProfileAccessory == null) {
			legacyProfileAccessory = new VBox();
			Label l = new Label(accessory.getDisplayLabel());
			l.getStyleClass().add("subtitle");
			Hyperlink link = new Hyperlink(bundle.getString("macros"));
			link.setOnAction((e) -> context.editMacros(LayoutControl.this));
			legacyProfileAccessory.getChildren().add(l);
			legacyProfileAccessory.getChildren().add(link);
		}
		return legacyProfileAccessory;
	}

	@Override
	protected void onBeforeSetEffectsControlDevice() {
		stack = new SlideyStack();
		JavaFX.clipChildren(stack, 0);
		deviceLayout = context.getLayouts().getLayout(getDevice());
		deviceLayout.addListener(this);
		views = deviceLayout.getViewsThatHave(ComponentType.AREA);
		overallEffect = new EffectBar(OVERALL_EFFECT_ICON_SIZE, getDevice(), context);
		overallEffect.setAlignment(Pos.CENTER);
		top.setCenter(overallEffect);
	}

	@Override
	public void removeSelectedElements() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<ComponentType> getEnabledTypes() {
		return enabledTypes.get();
	}

	@Override
	public void setEnabledTypes(List<ComponentType> enabledTypes) {
		this.enabledTypes.set(enabledTypes);
	}

	@Override
	public SimpleBooleanProperty selectableElements() {
		return selectableElements;
	}

	@Override
	public SimpleBooleanProperty readOnly() {
		return readOnly;
	}

	@FXML
	void evtPageLeft() {
		DeviceView view = views.get(getCurrentEditorIndex() - 1);
		LayoutEditor viewer = createEditor(view);
		stack.setContent(Direction.FROM_LEFT, viewer);
		fireViewChanged(viewer);
		rebuildRegions();
	}

	@FXML
	void evtPageRight() {
		DeviceView view = views.get(getCurrentEditorIndex() + 1);
		LayoutEditor viewer = createEditor(view);
		stack.setContent(Direction.FROM_RIGHT, viewer);
		fireViewChanged(viewer);
		rebuildRegions();
	}

	protected void fireViewChanged(ViewerView view) {
		for (int i = listeners.size() - 1; i >= 0; i--)
			listeners.get(i).viewerSelected(view);
	}

	@Override
	protected void onEffectChanged(Device device, uk.co.bithatch.snake.lib.Region region) {
		for (Node other : regions.values()) {
			if (other instanceof RegionControl)
				((RegionControl) other).update();
		}
		getCurrentEditor().reset();
		removeCustomEffectListener();
		checkForCustomEffect();
	}

	@Override
	protected void onEffectsControlCleanUp() {
		if (profileAccessory != null)
			profileAccessory.cleanUp();
		deviceLayout.removeListener(this);
		removeCustomEffectListener();
	}

	protected void removeCustomEffectListener() {
		if (currentEffect != null) {
			currentEffect.getPlayer().removeListener(this);
			currentEffect = null;
		}
	}

	protected void checkForCustomEffect() {
		EffectHandler<?, ?> mainEffect = context.getEffectManager().getRootAcquisition(getDevice())
				.getEffect(getDevice());
		if (mainEffect instanceof CustomEffectHandler) {
			currentEffect = (CustomEffectHandler) mainEffect;
			currentEffect.getPlayer().addListener(this);
		}
	}

	static class RegionControl extends VBox {
		private EffectBar effectBar;
		private Slider brightnessSlider;
		private Region region;
		private App context;

		RegionControl(DeviceView view, Region r, App context, EffectAcquisition acq, LayoutControl layoutControl) {
			this.region = r;
			this.context = context;

			Label l = new Label(LayoutEditor.getBestRegionName(view, r.getName()));
			l.getStyleClass().add("subtitle");
			HBox t = new HBox();
			t.getChildren().add(l);
			getChildren().add(t);

			EffectManager fx = context.getEffectManager();
			Set<EffectHandler<?, ?>> supported = fx.getEffects(r);

			Set<EffectHandler<?, ?>> allEffects = new LinkedHashSet<>(supported);
			Device device = r.getDevice();
			allEffects.addAll(fx.getEffects(r));
			EffectHandler<?, ?> selectedRegionEffect = acq.getEffect(r);

			if (!supported.isEmpty() && supported.contains(selectedRegionEffect)) {

				effectBar = new EffectBar(REGION_EFFECT_ICON_SIZE, 20, r, context);
				effectBar.maxWidth(80);
				effectBar.getStyleClass().add("small");

				for (EffectHandler<?, ?> f : allEffects) {
					if (!f.isMatrixBased()) {
						effectBar.addEffect(f);
						if (selectedRegionEffect != null && f == selectedRegionEffect) {
							effectBar.effect.set(f);
						}
					}
				}

				Hyperlink customise = new Hyperlink(bundle.getString("customiseRegion"));
				customise.setOnMouseClicked((e) -> layoutControl.customise(r, effectBar.effect.get()));
				customise.getStyleClass().add("small");
				setCustomiseState(customise, r, effectBar.effect.get());
				t.getChildren().add(customise);
				getChildren().add(effectBar);

				if (device.getCapabilities().contains(Capability.BRIGHTNESS_PER_REGION)) {
					if (r.getCapabilities().contains(Capability.BRIGHTNESS_PER_REGION)) {
						HBox hbox = new HBox();

						brightnessSlider = new Slider(0, 100, r.getBrightness());
						brightnessSlider.setUserData(r);
						brightnessSlider.maxWidth(80);
						Label la = new Label(String.format("%d%%", r.getBrightness()));
						la.getStyleClass().add("small");

						brightnessSlider.valueProperty().addListener((e) -> {
							r.setBrightness((short) brightnessSlider.valueProperty().get());
							la.textProperty().set(String.format("%d%%", r.getBrightness()));
						});

						hbox.getChildren().add(brightnessSlider);
						hbox.getChildren().add(la);
						getChildren().add(hbox);
					}
				}
			}

		}

		public void update() {
			if (brightnessSlider != null)
				brightnessSlider.valueProperty().set(region.getBrightness());
			if (effectBar != null)
				effectBar.effect
						.set(context.getEffectManager().getRootAcquisition(region.getDevice()).getEffect(region));
		}
	}

	@Override
	protected void rebuildRegions() {
		Device device = getDevice();
		List<Region> regionList = device.getRegions();
		EffectManager fx = context.getEffectManager();
		EffectAcquisition acq = fx.getRootAcquisition(device);
		regions.clear();
		LayoutEditor layoutEditor = getCurrentEditor();
		if (layoutEditor != null) {
			if (regionList.size() > 1) {
				EffectHandler<?, ?> selectedDeviceEffect = acq.getEffect(device);
				if (selectedDeviceEffect == null
						|| (selectedDeviceEffect != null && !selectedDeviceEffect.isMatrixBased())) {
					for (Region r : regionList) {
						regions.put(r.getName(),
								new RegionControl(layoutEditor.getView(), r, context, acq, LayoutControl.this));
					}
				}
			}
			layoutEditor.refresh();
		}
		int idx = getCurrentEditorIndex();
		pageLeft.visibleProperty().set(idx > 0);
		pageRight.visibleProperty().set(idx < views.size() - 1);
	}

	protected int getCurrentEditorIndex() {
		LayoutEditor le = getCurrentEditor();
		return le == null ? -1 : views.indexOf(le.getView());
	}

	protected LayoutEditor getCurrentEditor() {
		return (LayoutEditor) stack.getContent();
	}

	protected void setSelectedOverallEffect() {
		selectOverallEffect(currentEffect);
	}

	@Override
	protected void onRebuildOverallEffects() {
		overallEffect.getChildren().clear();
		EffectManager fx = context.getEffectManager();
		var deviceEffects = fx.getEffects(getDevice());
		EffectAcquisition acq = fx.getRootAcquisition(getDevice());
		EffectHandler<?, ?> selectedDeviceEffect = acq.getEffect(getDevice());
		overallEffect.effect.set(selectedDeviceEffect);
		for (EffectHandler<?, ?> effectHandler : deviceEffects) {
			overallEffect.addEffect(effectHandler);
		}
	}

	static class EffectBar extends HBox {
		private int iconSize;
		private SimpleObjectProperty<EffectHandler<?, ?>> effect = new SimpleObjectProperty<>(this, "effect");
		private Lit region;
		private App context;
		private int iconDisplaySize;

		EffectBar(int iconSize, Lit region, App context) {
			this(iconSize, iconSize, region, context);
		}

		EffectBar(int iconSize, int iconDisplaySize, Lit region, App context) {
			this.region = region;
			this.iconDisplaySize = iconDisplaySize;
			this.context = context;
			this.iconSize = iconSize;

			effect.addListener((e, o, n) -> {
				for (Node node : getChildren()) {
					EffectHandler<?, ?> effect = (EffectHandler<?, ?>) node.getUserData();
					if (Objects.equals(n, effect)) {
						select((Hyperlink) node);
					} else {
						deselect((Hyperlink) node);
					}
				}
			});
		}

		void addEffect(EffectHandler<?, ?> effect) {
			Hyperlink button = createButton(effect);
			getChildren().add(button);
		}

		Hyperlink createButton(EffectHandler<?, ?> effectHandler) {
			Hyperlink activate = new Hyperlink();
			activate.getStyleClass().add("deemphasis");
			Tooltip tt = new Tooltip(effectHandler.getDisplayName());
			tt.setShowDelay(Duration.millis(200));
			activate.setTooltip(tt);
			Node icon = effectHandler.getEffectImageNode(iconSize, iconDisplaySize);
			activate.setGraphic(icon);
			activate.setOnMouseClicked((e) -> {
				context.getEffectManager().getRootAcquisition(Lit.getDevice(region)).activate(region, effectHandler);
			});
			activate.setUserData(effectHandler);
			EffectHandler<?, ?> selectedDeviceEffect = effect.get();
			if (selectedDeviceEffect != null && effectHandler.equals(selectedDeviceEffect)) {
				select(activate);
			}
			return activate;
		}

		protected void select(Hyperlink activate) {
			activate.getStyleClass().remove("deemphasis");
			activate.setEffect(new Glow(SELECTED_GLOW_AMOUNT));
		}

		protected void deselect(Hyperlink activate) {
			if (!activate.getStyleClass().contains("deemphasis"))
				activate.getStyleClass().add("deemphasis");
			activate.setEffect(null);
		}
	}

	@Override
	protected void selectOverallEffect(EffectHandler<?, ?> effect) {
		overallEffect.effect.set(effect);
	}

	@Override
	protected EffectHandler<?, ?> getOverallEffect() {
		return overallEffect.effect.get();
	}

	@Override
	public void frameUpdate(KeyFrame frame, int[][][] rgb, float fac, long frameNumber) {
		getCurrentEditor().updateFromMatrix(rgb);
	}

	@Override
	public void pause(boolean pause) {
	}

	@Override
	public void started(Sequence sequence, Device device) {
	}

	@Override
	public void stopped() {
	}

	@Override
	public void addListener(ViewerListener listener) {
		listeners.add(listener);
	}

	@Override
	public void removeListener(ViewerListener listener) {
		listeners.remove(listener);

	}

	@Override
	public void layoutChanged(DeviceLayout layout, DeviceView view) {
		viewChanged();
	}

	@Override
	public void viewRemoved(DeviceLayout layout, DeviceView view) {
		viewChanged();
	}

	@Override
	public void viewChanged(DeviceLayout layout, DeviceView view) {
		viewChanged();
	}

	protected void viewChanged() {
		views = deviceLayout.getViewsThatHave(ComponentType.AREA);
		stack.getChildren().clear();
		if (views.isEmpty()) {
			fireViewChanged(null);
		} else {
			DeviceView view = views.get(0);
			LayoutEditor viewer = createEditor(view);
			stack.setContent(Direction.FADE, viewer);
			fireViewChanged(viewer);
		}
		rebuildRegions();
	}

	@Override
	public void viewElementAdded(DeviceLayout layout, DeviceView view, IO element) {
	}

	@Override
	public void viewElementChanged(DeviceLayout layout, DeviceView view, IO element) {
	}

	@Override
	public void viewElementRemoved(DeviceLayout layout, DeviceView view, IO element) {
	}

	@Override
	public void viewAdded(DeviceLayout layout, DeviceView view) {
	}

}
