package uk.co.bithatch.snake.ui;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Device.Listener;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.effects.Breath;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.lib.effects.Matrix;
import uk.co.bithatch.snake.lib.effects.Reactive;
import uk.co.bithatch.snake.lib.effects.Ripple;
import uk.co.bithatch.snake.lib.effects.Starlight;
import uk.co.bithatch.snake.lib.effects.Static;
import uk.co.bithatch.snake.lib.effects.Wave;
import uk.co.bithatch.snake.ui.SlideyStack.Direction;

public class EffectsControl extends ControlController implements Listener {
	@FXML
	private ComboBox<Class<? extends Effect>> overallEffect;
	@FXML
	private VBox regions;
	@FXML
	private Hyperlink customise;

	private boolean adjustingOverall = false;
	private boolean adjustingSingle = false;
	private Set<Class<? extends Effect>> deviceEffects;
	private List<ComboBox<Class<? extends Effect>>> others = new ArrayList<>();

	final static ResourceBundle bundle = ResourceBundle.getBundle(EffectsControl.class.getName());

	@Override
	protected void onSetDevice() {

		var device = getDevice();
		device.addListener(this);

		Callback<ListView<Class<? extends Effect>>, ListCell<Class<? extends Effect>>> cellFactory = createEffectCellFactory();
		overallEffect.setButtonCell(cellFactory.call(null));
		overallEffect.setCellFactory(cellFactory);

		rebuildOverallEffects();

		overallEffect.getSelectionModel().selectedItemProperty().addListener((e) -> {
			if (!adjustingOverall) {
				setCustomiseState(customise, device, overallEffect);
				Class<? extends Effect> clazz = overallEffect.getSelectionModel().getSelectedItem();
				if (clazz != null) {
					try {
						Effect effect = device.createEffect(clazz);
						context.getScheduler().execute(() -> device.setEffect(effect));
						adjustingSingle = true;
						try {
							for (ComboBox<Class<? extends Effect>> sl : others) {
								sl.getSelectionModel().select(clazz);
							}
						} finally {
							adjustingSingle = false;
						}
					} catch (Exception ex) {
						throw new IllegalStateException("Failed to create effect.", ex);
					}
				}
			}
		});
		rebuildRegions();
		setCustomiseState(customise, device, overallEffect);
	}

	private Callback<ListView<Class<? extends Effect>>, ListCell<Class<? extends Effect>>> createEffectCellFactory() {
		Callback<ListView<Class<? extends Effect>>, ListCell<Class<? extends Effect>>> cellFactory = new Callback<ListView<Class<? extends Effect>>, ListCell<Class<? extends Effect>>>() {

			@Override
			public ListCell<Class<? extends Effect>> call(ListView<Class<? extends Effect>> l) {
				return new ListCell<Class<? extends Effect>>() {

					@Override
					protected void updateItem(Class<? extends Effect> item, boolean empty) {
						super.updateItem(item, empty);
						if (item == null || empty) {
							setGraphic(null);
							setText(bundle.getString("emptyChoice"));
						} else {
							ImageView iv = new ImageView(context.getConfiguration().themeProperty().getValue()
									.getEffectImage(24, item).toExternalForm());
							iv.setFitHeight(22);
							iv.setFitWidth(22);
							iv.setSmooth(true);
							iv.setPreserveRatio(true);
							setGraphic(iv);
							setText(item.getSimpleName());
						}
					}
				};
			}
		};
		return cellFactory;
	}

	private void rebuildRegions() {
		others.clear();
		regions.getChildren().clear();
		List<Region> regionList = getDevice().getRegions();
		if (regionList.size() > 1) {
			for (Region r : regionList) {
				/* TODO: Special case for Matrix as it's a bit weird how it is implemented. */
				if (!r.getSupportedEffects().isEmpty()
						&& !(r.getSupportedEffects().size() == 1 && r.getSupportedEffects().contains(Matrix.class))) {
					HBox hbox = new HBox();

					ImageView iv = new ImageView(context.getConfiguration().themeProperty().getValue()
							.getRegionImage(24, r.getName()).toExternalForm());
					iv.setFitHeight(22);
					iv.setFitWidth(22);
					iv.setSmooth(true);
					iv.setPreserveRatio(true);

					Hyperlink customise = new Hyperlink(bundle.getString("customize"));
					ComboBox<Class<? extends Effect>> br = new ComboBox<>();
					Callback<ListView<Class<? extends Effect>>, ListCell<Class<? extends Effect>>> cellFactory = createEffectCellFactory();
					br.setButtonCell(cellFactory.call(null));
					br.setCellFactory(cellFactory);
					br.maxWidth(80);
					br.getStyleClass().add("small");
					Set<Class<? extends Effect>> allEffects = new LinkedHashSet<>(r.getSupportedEffects());
					allEffects.addAll(getDevice().getSupportedEffects());
					Effect selectedDeviceEffect = getDevice().getEffect();
					Effect selectedRegionEffect = r.getEffect();
					if (r.getSupportedEffects().contains(selectedDeviceEffect.getClass())) {
						for (Class<? extends Effect> f : allEffects) {
							br.itemsProperty().get().add(f);
							if (selectedRegionEffect != null && f == selectedRegionEffect.getClass()) {
								br.getSelectionModel().select(f);
							}
						}
					} else {
						br.itemsProperty().get().add(selectedDeviceEffect.getClass());
						br.getSelectionModel().select(selectedDeviceEffect.getClass());
						br.disableProperty().set(true);
					}
					br.getSelectionModel().selectedItemProperty().addListener((e) -> {
						var efClazz = br.getSelectionModel().getSelectedItem();
						if (!adjustingSingle && r.getSupportedEffects().contains(efClazz)) {
							Effect effect = getDevice().createEffect(efClazz);
							context.getScheduler().execute(() -> r.setEffect(effect));
							adjustingOverall = true;
							try {
								overallEffect.getSelectionModel().select(getDevice().getEffect().getClass());
								setCustomiseState(customise, r, br);
							} finally {
								adjustingOverall = false;
							}
						}
					});
					others.add(br);

					customise.getStyleClass().add("smallIconButton");
					customise.onActionProperty().set((e) -> {
						customise(r, br);
					});
					setCustomiseState(customise, r, br);

					hbox.getChildren().add(iv);
					hbox.getChildren().add(br);
					hbox.getChildren().add(customise);
					regions.getChildren().add(hbox);
				}
			}
		}
	}

	private void rebuildOverallEffects() {

		overallEffect.itemsProperty().get().clear();
		deviceEffects = getDevice().getSupportedEffects();
		Effect selectedDeviceEffect = getDevice().getEffect();
		adjustingOverall = true;
		try {
			for (Class<? extends Effect> f : deviceEffects) {
				overallEffect.itemsProperty().get().add(f);
				if (selectedDeviceEffect != null && f == selectedDeviceEffect.getClass()) {
					overallEffect.getSelectionModel().select(f);
				}
			}
		} finally {
			adjustingOverall = false;
		}
	}

	@FXML
	private void evtCustomise(ActionEvent evt) {
		customise(getDevice(), overallEffect);
	}

	@SuppressWarnings("unchecked")
	private void customise(Lit region, ComboBox<Class<? extends Effect>> menu) {
		AbstractEffectController<Effect> c = null;
		var clazz = menu.getSelectionModel().getSelectedItem();
		var efClazz = getOptionsClassForEffect(clazz);
		if (efClazz != null) {
			c = (AbstractEffectController<Effect>) context.push(efClazz, this, Direction.FROM_RIGHT);
			try {
				Effect efct = region.getEffect();
				if (efct.getClass() != clazz) {
					efct = region.createEffect(clazz);
				}
				c.setRegion(region);
				c.setEffect(efct);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private Class<? extends AbstractEffectController<? extends Effect>> getOptionsClassForEffect(
			Class<? extends Effect> clazz) {
		Class<? extends AbstractEffectController<? extends Effect>> efClazz = null;
		if (clazz.equals(Breath.class)) {
			efClazz = BreathOptions.class;
		} else if (clazz.equals(Wave.class)) {
			efClazz = WaveOptions.class;
		} else if (clazz.equals(Static.class)) {
			efClazz = StaticOptions.class;
		} else if (clazz.equals(Reactive.class)) {
			efClazz = ReactiveOptions.class;
		} else if (clazz.equals(Matrix.class)) {
			efClazz = MatrixOptions.class;
		} else if (clazz.equals(Starlight.class)) {
			efClazz = StarlightOptions.class;
		} else if (clazz.equals(Ripple.class)) {
			efClazz = RippleOptions.class;
		}
		return efClazz;
	}

	@Override
	public void changed(Device device, Region region) {
		if (!Platform.isFxApplicationThread())
			Platform.runLater(() -> changed(device, region));
		else {
			if (region == null) {
				rebuildOverallEffects();
				rebuildRegions();
			}
		}
	}

	@Override
	protected void onCleanUp() {
		getDevice().removeListener(this);
	}

	void setCustomiseState(Node customise, Lit region, ComboBox<Class<? extends Effect>> menu) {
		var selectedItem = menu.getSelectionModel().getSelectedItem();
		if (selectedItem != null) {
			var efClazz = getOptionsClassForEffect(selectedItem);
			customise.visibleProperty().set(efClazz != null && region.getSupportedEffects().contains(selectedItem));
		}
	}

}
