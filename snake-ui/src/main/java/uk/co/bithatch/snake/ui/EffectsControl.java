package uk.co.bithatch.snake.ui;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

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
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.ui.effects.EffectAcquisition;
import uk.co.bithatch.snake.ui.effects.EffectManager;

public class EffectsControl extends AbstractEffectsControl {
	@FXML
	private ComboBox<EffectHandler<?, ?>> overallEffect;
	@FXML
	private VBox regions;

	private boolean adjustingOverall = false;
	private boolean adjustingSingle = false;
	private Set<EffectHandler<?, ?>> deviceEffects;
	private Map<Region, ComboBox<EffectHandler<?, ?>>> others = new LinkedHashMap<>();

	final static ResourceBundle bundle = ResourceBundle.getBundle(EffectsControl.class.getName());

	@Override
	protected void onSetEffectsControlDevice() {
		var device = getDevice();

		Callback<ListView<EffectHandler<?, ?>>, ListCell<EffectHandler<?, ?>>> cellFactory = createEffectCellFactory();
		overallEffect.setButtonCell(cellFactory.call(null));
		overallEffect.setCellFactory(cellFactory);

		overallEffect.getSelectionModel().selectedItemProperty().addListener((e) -> {
			if (!adjustingOverall) {
				setCustomiseState(customise, device, overallEffect.getSelectionModel().getSelectedItem());
				context.getEffectManager().getRootAcquisition(device).activate(device,
						overallEffect.getSelectionModel().getSelectedItem());
			}
		});
	}

	private Callback<ListView<EffectHandler<?, ?>>, ListCell<EffectHandler<?, ?>>> createEffectCellFactory() {
		Callback<ListView<EffectHandler<?, ?>>, ListCell<EffectHandler<?, ?>>> cellFactory = new Callback<ListView<EffectHandler<?, ?>>, ListCell<EffectHandler<?, ?>>>() {

			@Override
			public ListCell<EffectHandler<?, ?>> call(ListView<EffectHandler<?, ?>> l) {
				return new ListCell<EffectHandler<?, ?>>() {

					@Override
					protected void updateItem(EffectHandler<?, ?> item, boolean empty) {
						super.updateItem(item, empty);
						if (item == null || empty) {
							setGraphic(null);
							setText(bundle.getString("emptyChoice"));
						} else {
							Node effectImageNode = item.getEffectImageNode(24, 22);
							effectImageNode.getStyleClass().add("cell");
							setGraphic(effectImageNode);
							setText(item.getDisplayName());
						}
					}
				};
			}
		};
		return cellFactory;
	}

	protected void rebuildRegions() {
		others.clear();
		regions.getChildren().clear();
		Device device = getDevice();
		List<Region> originalRegionList = device.getRegions();
		List<Region> regionList = new ArrayList<>();
		EffectManager fx = context.getEffectManager();
		EffectAcquisition acq = fx.getRootAcquisition(device);
		EffectHandler<?, ?> selectedDeviceEffect = acq.getEffect(device);
		for (Region r : originalRegionList) {
			Set<EffectHandler<?, ?>> supported = fx.getEffects(r);
			EffectHandler<?, ?> selectedRegionEffect = acq.getEffect(r);
			if (!supported.isEmpty() && supported.contains(selectedRegionEffect)) {
				regionList.add(r);
			}
		}

		if (regionList.size() > 1) {
			if (selectedDeviceEffect != null && !selectedDeviceEffect.isMatrixBased()) {
				for (Region r : regionList) {
					Set<EffectHandler<?, ?>> supported = fx.getEffects(r);

					Set<EffectHandler<?, ?>> allEffects = new LinkedHashSet<>(supported);
					allEffects.addAll(fx.getEffects(device));
					EffectHandler<?, ?> selectedRegionEffect = acq.getEffect(r);
					HBox hbox = new HBox();

					ImageView iv = new ImageView(context.getConfiguration().themeProperty().getValue()
							.getRegionImage(24, r.getName()).toExternalForm());
					iv.setFitHeight(22);
					iv.setFitWidth(22);
					iv.setSmooth(true);
					iv.setPreserveRatio(true);

					Hyperlink customise = new Hyperlink(bundle.getString("customise"));
					ComboBox<EffectHandler<?, ?>> br = new ComboBox<>();
					Callback<ListView<EffectHandler<?, ?>>, ListCell<EffectHandler<?, ?>>> cellFactory = createEffectCellFactory();
					br.setButtonCell(cellFactory.call(null));
					br.setCellFactory(cellFactory);
					br.maxWidth(80);
					br.getStyleClass().add("small");

					for (EffectHandler<?, ?> f : allEffects) {
						if (!f.isMatrixBased()) {
							br.itemsProperty().get().add(f);
							if (selectedRegionEffect != null && f == selectedRegionEffect) {
								br.getSelectionModel().select(f);
							}
						}
					}
					br.getSelectionModel().selectedItemProperty().addListener((e) -> {
						var efHandler = br.getSelectionModel().getSelectedItem();
						if (!adjustingSingle && supported.contains(efHandler)) {

							acq.activate(r, efHandler);

							adjustingOverall = true;
							try {
								overallEffect.getSelectionModel().select(acq.getEffect(device));
								setCustomiseState(customise, r, br.getSelectionModel().getSelectedItem());
							} finally {
								adjustingOverall = false;
							}
						}
					});
					others.put(r, br);

					customise.getStyleClass().add("smallIconButton");
					customise.onActionProperty().set((e) -> {
						customise(r, br.getSelectionModel().getSelectedItem());
					});
					setCustomiseState(customise, r, br.getSelectionModel().getSelectedItem());

					hbox.getChildren().add(iv);
					hbox.getChildren().add(br);
					hbox.getChildren().add(customise);
					regions.getChildren().add(hbox);
				}
			}
		}
	}

	protected void onRebuildOverallEffects() {
		overallEffect.itemsProperty().get().clear();
		EffectManager fx = context.getEffectManager();
		deviceEffects = fx.getEffects(getDevice());
		EffectAcquisition acq = fx.getRootAcquisition(getDevice());
		EffectHandler<?, ?> selectedDeviceEffect = acq.getEffect(getDevice());
		for (EffectHandler<?, ?> f : deviceEffects) {
			overallEffect.itemsProperty().get().add(f);
			if (selectedDeviceEffect != null && f.equals(selectedDeviceEffect)) {
				overallEffect.getSelectionModel().select(f);
			}
		}
	}

	@Override
	protected void selectOverallEffect(EffectHandler<?, ?> effect) {
		overallEffect.getSelectionModel().select(effect);
	}

	@Override
	protected EffectHandler<?, ?> getOverallEffect() {
		return overallEffect.getSelectionModel().getSelectedItem();
	}

}
