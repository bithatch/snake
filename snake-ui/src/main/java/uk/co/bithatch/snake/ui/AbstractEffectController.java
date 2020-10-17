package uk.co.bithatch.snake.ui;

import java.util.ServiceLoader;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Effect;

public abstract class AbstractEffectController<E extends Effect> extends AbstractDeviceController {

	public static boolean hasController(Class<? extends Effect> effect) {
		return getControllerForEffect(effect) != null;
	}
	
	@SuppressWarnings("unchecked")
	public static <E extends Effect> Class<? extends AbstractEffectController<E>> getControllerForEffect(Class<E> effect) {
		for (EffectOptions<E> eo : ServiceLoader.load(EffectOptions.class)) {
			if(eo.getEffectClass().equals(effect))
				return eo.getOptionsController();
		}
		return null;
	}
	
	
	@FXML
	private Label deviceName;
	@FXML
	private ImageView deviceImage;
	@FXML
	private BorderPane header;

	private E effect;
	private Lit region;

	public E getEffect() {
		return effect;
	}

	public final void setEffect(E effect) {
		this.effect = effect;
		onSetEffect();
	}

	@Override
	protected final void onSetDevice() throws Exception {
		header.setBackground(createHeaderBackground());
		deviceImage.setImage(new Image(getDevice().getImage(), true));
		deviceName.textProperty().set(getDevice().getName());
		onSetEffectDevice();
	}

	protected void onSetEffectDevice() {
	}

	protected void onSetEffect() {
	}

	public Lit getRegion() {
		return region;
	}

	public final void setRegion(Lit region) {
		this.region = region;
		onSetRegion();
	}

	protected void onSetRegion() {
	}

}
