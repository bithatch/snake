package uk.co.bithatch.snake.ui;

import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.ui.effects.CustomEffectHandler;
import uk.co.bithatch.snake.ui.effects.EffectAcquisition.EffectChangeListener;
import uk.co.bithatch.snake.ui.effects.EffectManager;
import uk.co.bithatch.snake.ui.effects.EffectManager.Listener;
import uk.co.bithatch.snake.ui.util.JavaFX;
import uk.co.bithatch.snake.ui.widgets.Direction;

public abstract class AbstractEffectsControl extends ControlController implements Listener, EffectChangeListener {
	@FXML
	protected Hyperlink customise;
	@FXML
	protected Hyperlink addCustom;
	@FXML
	protected Hyperlink removeCustom;

	protected boolean adjustingOverall = false;
	protected boolean adjustingSingle = false;

	final static ResourceBundle bundle = ResourceBundle.getBundle(AbstractEffectsControl.class.getName());

	@Override
	protected final void onSetControlDevice() {
		onBeforeSetEffectsControlDevice();
		var device = getDevice();

		JavaFX.bindManagedToVisible(addCustom, customise, removeCustom);
		EffectManager fx = context.getEffectManager();
		addCustom.visibleProperty().set(fx.isSupported(getDevice(), CustomEffectHandler.class));
		removeCustom.visibleProperty()
				.set(fx.getRootAcquisition(getDevice()).getEffect(getDevice()) instanceof CustomEffectHandler);

		rebuildOverallEffects();

		rebuildRegions();
		setCustomiseState(customise, device, getOverallEffect());
		fx.addListener(this);
		fx.getRootAcquisition(getDevice()).addListener(this);
		onSetEffectsControlDevice();
	}

	protected void onBeforeSetEffectsControlDevice() {

	}

	protected void onSetEffectsControlDevice() {

	}

	protected abstract void rebuildRegions();

	protected final void rebuildOverallEffects() {
		adjustingOverall = true;
		try {
			onRebuildOverallEffects();
		} finally {
			adjustingOverall = false;
		}
	}

	protected abstract void onRebuildOverallEffects();

	@FXML
	protected final void evtCustomise() {
		customise(getDevice(), getOverallEffect());
	}

	@FXML
	protected final void evtRemoveCustom() {
		Confirm confirm = context.push(Confirm.class, Direction.FADE);
		confirm.confirm(bundle, "removeCustom", () -> {
			context.getEffectManager().remove(getOverallEffect());
		}, getOverallEffect().getName());
	}

	@FXML
	protected final void evtAddCustom() {
		Input confirm = context.push(Input.class, Direction.FADE);
		confirm.setValidator((l) -> {
			String name = confirm.inputProperty().get();
			boolean available = context.getEffectManager().getEffect(getDevice(), name) == null;
			l.getStyleClass().clear();
			if (available) {
				l.textProperty().set("");
				l.visibleProperty().set(false);
			} else {
				l.visibleProperty().set(true);
				l.textProperty().set(bundle.getString("error.nameAlreadyExists"));
				l.getStyleClass().add("danger");
			}
			return available;
		});
		confirm.confirm(bundle, "addCustom", () -> {
			CustomEffectHandler fx = new CustomEffectHandler(confirm.inputProperty().get());
			context.getEffectManager().add(getDevice(), fx);
			context.getEffectManager().getRootAcquisition(getDevice()).activate(getDevice(), fx);
		});
	}

	@SuppressWarnings("unchecked")
	protected void customise(Lit region, EffectHandler<?, ?> handler) {
		AbstractEffectController<?, EffectHandler<?, ?>> c = null;
		if (handler != null && handler.hasOptions()) {
			c = (AbstractEffectController<?, EffectHandler<?, ?>>) context.push(handler.getOptionsController(), this,
					Direction.FROM_RIGHT);
			try {
				c.setRegion(region);
				c.setEffectHandler(handler);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected final void onDeviceCleanUp() {
		context.getEffectManager().getRootAcquisition(getDevice()).removeListener(this);
		context.getEffectManager().removeListener(this);
		onEffectsControlCleanUp();
	}

	protected void onEffectsControlCleanUp() {

	}

	protected static void setCustomiseState(Node customise, Lit region, EffectHandler<?, ?> selectedItem) {
		customise.visibleProperty()
				.set(selectedItem != null && selectedItem.isSupported(region) && selectedItem.hasOptions());
	}

	@Override
	public final void effectChanged(Lit component, EffectHandler<?, ?> effect) {
		if (!Platform.isFxApplicationThread())
			Platform.runLater(() -> effectChanged(component, effect));
		else {
			if (component instanceof Device) {
				if (component instanceof Device)
					selectOverallEffect(effect);
				setCustomiseState(customise, getDevice(), getOverallEffect());
				removeCustom.visibleProperty().set(effect instanceof CustomEffectHandler);
				rebuildRegions();
			}
		}

	}

	protected abstract void selectOverallEffect(EffectHandler<?, ?> effect);

	protected abstract EffectHandler<?, ?> getOverallEffect();

	@Override
	public final void effectRemoved(Device device, EffectHandler<?, ?> effect) {
		if (!Platform.isFxApplicationThread())
			Platform.runLater(() -> effectRemoved(device, effect));
		else {
			rebuildOverallEffects();
			rebuildRegions();
		}
	}

	@Override
	public final void effectAdded(Device device, EffectHandler<?, ?> effect) {
		if (!Platform.isFxApplicationThread())
			Platform.runLater(() -> effectRemoved(device, effect));
		else {
			rebuildOverallEffects();
			rebuildRegions();
		}
	}

}
