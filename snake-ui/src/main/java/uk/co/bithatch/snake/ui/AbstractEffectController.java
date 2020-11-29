package uk.co.bithatch.snake.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import uk.co.bithatch.snake.lib.Lit;

public abstract class AbstractEffectController<E, H extends EffectHandler<E, ?>> extends AbstractDeviceController {

	@FXML
	private ImageView deviceImage;
	@FXML
	private Label deviceName;
	private H effectHandler;

	@FXML
	private BorderPane header;
	private Lit region;

	public Lit getRegion() {
		return region;
	}

	public final void setEffectHandler(H effect) {
		this.effectHandler = effect;
		onSetEffectHandler();
	}

	public final void setRegion(Lit region) {
		this.region = region;
		onSetRegion();
	}

	protected H getEffectHandler() {
		return effectHandler;
	}

	protected String getHeaderTitle() {
		return getDevice().getName();
	}

	@Override
	protected final void onSetDevice() throws Exception {
		header.setBackground(createHeaderBackground());
		deviceImage.setImage(new Image(context.getDefaultImage(getDevice().getType(),
				context.getCache().getCachedImage(getDevice().getImage())), true));
		deviceName.textProperty().set(getDevice().getName());
		onSetEffectDevice();
	}

	protected void onSetEffectDevice() {
	}

	protected void onSetEffectHandler() {
	}

	protected void onSetRegion() {
	}
}
