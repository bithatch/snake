package uk.co.bithatch.snake.ui;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;

import com.sshtools.icongenerator.IconBuilder;

import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.control.SplitPane;
import javafx.scene.effect.Glow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.FramePlayer;
import uk.co.bithatch.snake.lib.FramePlayer.FrameListener;
import uk.co.bithatch.snake.lib.Interpolation;
import uk.co.bithatch.snake.lib.KeyFrame;
import uk.co.bithatch.snake.lib.Sequence;
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.IO;
import uk.co.bithatch.snake.lib.layouts.MatrixIO;
import uk.co.bithatch.snake.ui.addons.CustomEffect;
import uk.co.bithatch.snake.ui.designer.MatrixView;
import uk.co.bithatch.snake.ui.designer.TabbedViewer;
import uk.co.bithatch.snake.ui.effects.CustomEffectHandler;
import uk.co.bithatch.snake.ui.util.JavaFX;
import uk.co.bithatch.snake.ui.util.Strings;
import uk.co.bithatch.snake.ui.util.Time;
import uk.co.bithatch.snake.ui.widgets.Direction;

public class CustomOptions extends AbstractEffectController<Sequence, CustomEffectHandler> implements FrameListener {

	private static final double MIN_SPEED = 0.001;

	final static Preferences PREFS = Preferences.userNodeForPackage(CustomOptions.class);
	final static String PREF_TIMELINE_VISIBLE = "timelineVisible";
	final static String PREF_TIMELINE_DIVIDER = "timelineDivider";

	final static ResourceBundle bundle = ResourceBundle.getBundle(CustomOptions.class.getName());

	@FXML
	private Hyperlink addFrame;
	@FXML
	private ColorPicker color;
	@FXML
	private Label colorLabel;
	@FXML
	private BorderPane container;
	@FXML
	private BorderPane timelineContainer;
	@FXML
	private ComboBox<Interpolation> defaultInterpolation;
	@FXML
	private Label effectName;
	@FXML
	private Spinner<Integer> fps;
	@FXML
	private Label frames;
	@FXML
	private Spinner<Double> holdKeyFrameFor;
	@FXML
	private ComboBox<Interpolation> keyFrameInterpolation;
	@FXML
	private Hyperlink pause;
	@FXML
	private Hyperlink play;
	private KeyFrame playingFrame;
	@FXML
	private Slider progress;
	@FXML
	private Hyperlink removeFrame;
	@FXML
	private CheckBox repeat;
	@FXML
	private Spinner<Double> speed;
	@FXML
	private Spinner<Integer> keyFrameNumber;
	@FXML
	private Hyperlink stop;
	@FXML
	private Label time;
	@FXML
	private HBox timeline;
	@FXML
	private Button reset;
	@FXML
	private Button selectAll;
	@FXML
	private Button export;
	@FXML
	private Button removeEffect;
	@FXML
	private Button shiftLeft;
	@FXML
	private Button shiftRight;
	@FXML
	private Button shiftUp;
	@FXML
	private Button shiftDown;
	@FXML
	private SplitPane split;
	@FXML
	private Hyperlink showTimeline;
	@FXML
	private Hyperlink hideTimeline;

	@FXML
	private ScrollPane timelineScrollPane;

	private TabbedViewer deviceViewer;
	private boolean adjusting;
	private boolean adjustingProgress;
	private int deviceX;
	private int deviceY;
	private Map<KeyFrame, BorderPane> keyFrames = new HashMap<>();
	private boolean keyFrameAdjusting;
	private boolean timelineHidden;
	private ObjectProperty<KeyFrame> currentKeyFrame = new SimpleObjectProperty<>(null, "currentKeyFrame");

	private IntegerSpinnerValueFactory keyFrameNumberFactory;

	@Override
	public void frameUpdate(KeyFrame frame, int[][][] rgb, float fac, long frameNumber) {
		Platform.runLater(() -> {
			updateFrame(rgb);
			rebuildTimestats();
		});
	}

	@Override
	public void pause(boolean pause) {
		Platform.runLater(() -> {
			deviceViewer.setSelectableElements(!pause);
			updateAvailability();
			rebuildTimestats();
		});
	}

	@Override
	public void started(Sequence sequence, Device device) {
		Platform.runLater(() -> {
			deviceViewer.setSelectableElements(false);
			updateAvailability();
			rebuildTimestats();
		});
	}

	@Override
	public void stopped() {
		Platform.runLater(() -> {
			deviceViewer.setSelectableElements(true);
			updateAvailability();
			rebuildTimestats();
			progress.valueProperty().set(0);
		});
	}

	protected void addFrame(int index) {
		KeyFrame f = new KeyFrame();
		f.setHoldFor(5000);
		f.setColor(new int[] { 0xff, 0xff, 0xff }, deviceY, deviceX);
		Sequence seq = getEffectHandler().getSequence();
		seq.add(index, f);
		keyFrameNumberFactory.setMax(seq.size() - 1);
		rebuildTimeline();
		rebuildTimestats();
		updateAvailability();
		selectFrame(f);
		saveSequence();
	}

	protected void confirmRemoveFrame(KeyFrame frame, boolean unpause) {
		CustomEffectHandler fx = getEffectHandler();
		Confirm confirm = context.push(Confirm.class, Direction.FADE);
		confirm.confirm(bundle, "removeFrame", () -> {
			BorderPane bp = keyFrames.get(frame);
			JavaFX.fadeHide(bp, 1, (e) -> {
				timeline.getChildren().remove(bp);
				Sequence seq = fx.getSequence();
				seq.remove(frame);
				keyFrameNumberFactory.setMax(Math.max(0, seq.size() - 1));
				updateAvailability();
				rebuildTimestats();
				rebuildTimeline();
				if (unpause)
					fx.getPlayer().setPaused(false);
			});
		}, () -> {
			if (unpause)
				fx.getPlayer().setPaused(false);
		}, fx.getSequence().indexOf(frame));
	}

	protected ContextMenu creatContextMenu(KeyFrame frame, Hyperlink button) {
		ContextMenu menu = new ContextMenu();

		// Remove
		MenuItem remove = new MenuItem(bundle.getString("contextMenuRemove"));
		remove.onActionProperty().set((e) -> removeFrame(frame));
		menu.getItems().add(remove);

		// Copy
		MenuItem copy = new MenuItem(bundle.getString("contextMenuCopy"));
		copy.onActionProperty().set((e) -> {
		});
		menu.getItems().add(copy);

		// Cut
		MenuItem cut = new MenuItem(bundle.getString("contextMenuCut"));
		cut.onActionProperty().set((e) -> {
		});
		menu.getItems().add(cut);

		// Paste After
		MenuItem pasteAfter = new MenuItem(bundle.getString("contextMenuPasteAfter"));
		pasteAfter.onActionProperty().set((e) -> {
		});
		menu.getItems().add(pasteAfter);

		// Paste Before
		MenuItem pasteBefore = new MenuItem(bundle.getString("contextMenuPasteBefore"));
		pasteBefore.onActionProperty().set((e) -> {
		});
		menu.getItems().add(pasteBefore);

		// Paste Over
		MenuItem pasteOver = new MenuItem(bundle.getString("contextMenuPasteOver"));
		pasteOver.onActionProperty().set((e) -> {
		});
		menu.getItems().add(pasteOver);

		// Add After
		MenuItem addAfter = new MenuItem(bundle.getString("contextMenuAddAfter"));
		addAfter.onActionProperty().set((e) -> addFrame(getEffectHandler().getSequence().indexOf(frame) + 1));
		menu.getItems().add(addAfter);

		// Add Before
		MenuItem addBefore = new MenuItem(bundle.getString("contextMenuAddBefore"));
		addBefore.onActionProperty().set((e) -> addFrame(getEffectHandler().getSequence().indexOf(frame)));
		menu.getItems().add(addBefore);

		button.setOnContextMenuRequested((e) -> {
			FramePlayer player = getEffectHandler().getPlayer();
			remove.disableProperty().set(player.isPlaying());
			addAfter.disableProperty().set(player.isPlaying());
			addBefore.disableProperty().set(player.isPlaying());
			pasteOver.disableProperty().set(player.isPlaying());
			pasteBefore.disableProperty().set(player.isPlaying());
			pasteAfter.disableProperty().set(player.isPlaying());
			cut.disableProperty().set(player.isPlaying());
			copy.disableProperty().set(player.isPlaying());
		});

		return menu;
	}

	@Override
	protected void onDeviceCleanUp() {
		CustomEffectHandler fx = getEffectHandler();
		FramePlayer player = fx.getPlayer();
		if (context.getEffectManager().getDeviceEffectHandlers(getDevice()).containsValue(fx)) {
			fx.activate(getDevice());
		}
		player.removeListener(this);
		deviceViewer.cleanUp();
	}

	@Override
	protected void onSetEffectDevice() {

		int[] dim = getDevice().getMatrixSize();
		deviceY = dim[0];
		deviceX = dim[1];

		colorLabel.setLabelFor(color);

		for (Interpolation ip : Interpolation.interpolations()) {
			if (!ip.equals(Interpolation.sequence))
				defaultInterpolation.itemsProperty().get().add(ip);
			keyFrameInterpolation.itemsProperty().get().add(ip);
		}

		deviceViewer = new TabbedViewer(context, getDevice());
		container.setCenter(deviceViewer);

		deviceViewer.setReadOnly(true);
		deviceViewer.setEnabledTypes(Arrays.asList(ComponentType.LED, ComponentType.AREA));
		deviceViewer.setSelectionMode(SelectionMode.MULTIPLE);
		deviceViewer.setSelectableElements(true);
		deviceViewer.setLayout(context.getLayouts().getLayout(getDevice()));
		deviceViewer.getKeySelectionModel().getSelectedItems().addListener(new ListChangeListener<>() {
			@Override
			public void onChanged(Change<? extends IO> c) {
				setColorForButtons();
				updateAvailability();
			}
		});

		timelineContainer.managedProperty().bind(timelineContainer.visibleProperty());
		hideTimeline.managedProperty().bind(hideTimeline.visibleProperty());
		hideTimeline.visibleProperty().bind(timelineContainer.visibleProperty());
		showTimeline.managedProperty().bind(showTimeline.visibleProperty());
		showTimeline.visibleProperty().bind(Bindings.not(timelineContainer.visibleProperty()));

	}

	@Override
	protected void onSetEffectHandler() {
		CustomEffectHandler effect = getEffectHandler();
		FramePlayer player = effect.getPlayer();
		deviceViewer.setSelectableElements(!player.isActive());

		effectName.textProperty().set(effect.getDisplayName());
		 keyFrameNumberFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Math.max(0, effect.getSequence().size() - 1), 1, 1);
		keyFrameNumber.setValueFactory(keyFrameNumberFactory);
		keyFrameNumber.valueProperty().addListener((e) -> {
			if (!keyFrameAdjusting)
				selectFrame(effect.getSequence().get(keyFrameNumber.valueProperty().get()));
		});

		speed.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(MIN_SPEED, 500, 1, 1));
		speed.valueProperty().addListener((e) -> {
			if (speed.valueProperty().get() < MIN_SPEED) {
				speed.getValueFactory().setValue(MIN_SPEED);
			} else {
				effect.getSequence().setSpeed(speed.valueProperty().get().floatValue());
				rebuildTimestats();
				saveSequence();
			}
		});
		fps.setValueFactory(
				new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 50, effect.getSequence().getFps(), 1));
		fps.valueProperty().addListener((e) -> {
			effect.getSequence().setFps(fps.valueProperty().get());
			rebuildTimestats();
			saveSequence();
		});
		repeat.selectedProperty().set(effect.getSequence().isRepeat());
		defaultInterpolation.selectionModelProperty().get().select(effect.getSequence().getInterpolation());
		keyFrameInterpolation.selectionModelProperty().addListener((e) -> {
			if (!keyFrameAdjusting) {
				getSelectedFrame().setInterpolation(keyFrameInterpolation.getSelectionModel().getSelectedItem());
				saveSequence();
			}
		});

		holdKeyFrameFor.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 5000, 1, 1));
		holdKeyFrameFor.valueProperty().addListener((e) -> {
			if (!keyFrameAdjusting) {
				getSelectedFrame().setHoldFor((long) (holdKeyFrameFor.getValue() * 1000.0));
				saveSequence();
			}
		});
		currentKeyFrame.addListener((e, o, n) -> {
			setColorForButtons();
		});

		rebuildTimeline();
		updateAvailability();
		rebuildTimestats();

		player.addListener(this);

		double pos = PREFS.getDouble(PREF_TIMELINE_DIVIDER, -1);
		boolean vis = PREFS.getBoolean(PREF_TIMELINE_VISIBLE, true);
		split.getDividers().get(0).positionProperty().addListener((e,o,n) -> {
			if (timelineContainer.visibleProperty().get()) {
				PREFS.putDouble(PREF_TIMELINE_DIVIDER, split.getDividerPositions()[0]);
			} else if (timelineHidden && n.floatValue() > 1) {
				evtShowTimeline();
			}
		});
		if (vis) {
			split.setDividerPositions(pos == -1 ? 0.75 : pos);
		}
		else {
			timelineHidden = true;
			timelineContainer.visibleProperty().set(false);
			split.setDividerPositions(1);
		}

		progress.valueProperty().addListener((e) -> {
			if (!adjustingProgress) {
				try {
					adjustingProgress = true;
					long t = (long) (progress.valueProperty().get() * 1000.0);
					if (player.isPaused()) {
						player.setTimeElapsed(t);
					} else {
						KeyFrame f = effect.getSequence().getFrameAt(t);
						if (!player.isPlaying())
							updateKeyFrameConfiguration(f);
						updateFrame(f.getFrame());
					}
				} finally {
					adjustingProgress = false;
				}
			}
		});

	}

	protected void removeFrame(KeyFrame frame) {
		FramePlayer player = getEffectHandler().getPlayer();
		boolean unpause = false;
		if (player.isPlaying() && !player.isPaused()) {
			player.setPaused(true);
			unpause = true;
		}
		confirmRemoveFrame(frame, unpause);
	}

	protected void saveSequence() {
		getEffectHandler().store(getDevice(), this);
	}

	protected void selectFrame(KeyFrame frame) {
		FramePlayer player = getEffectHandler().getPlayer();
		if (player.isPlaying())
			player.setTimeElapsed(frame.getIndex());

		progress.valueProperty().set((double) frame.getIndex() / 1000.0);
		updateKeyFrameConfiguration(frame);
		rebuildTimestats();
	}

	protected void updateFrame(int[][][] rgb) {
		deviceViewer.updateFromMatrix(rgb);
	}

	protected void updateKeyFrameConfiguration(KeyFrame frame) {
		keyFrameAdjusting = true;
		try {
			currentKeyFrame.set(frame);
			keyFrameNumber.valueFactoryProperty().get().setValue(frame.getSequence().indexOf(frame));
			keyFrameInterpolation.getSelectionModel().select(frame.getInterpolation());
			holdKeyFrameFor.getValueFactory().valueProperty().set((double) frame.getHoldFor() / 1000);
		} finally {
			keyFrameAdjusting = false;
		}
	}

	Canvas buildFrameIcon(KeyFrame frame) {
		IconBuilder builder = new IconBuilder();
		builder.width(64);
		builder.height(64);
		builder.roundRect(32);
		builder.text(Time.formatTime(frame.getIndex()));
		int[] rgb = frame.getOverallColor();
		int cval = JavaFX.encodeRGB(rgb[0], rgb[1], rgb[2]);
		builder.autoTextColorPreferWhite();
		builder.color(cval);
		builder.fontSize(8);
		Canvas icon = builder.build(Canvas.class);
		return icon;
	}

	@FXML
	void evtAddFrame() {
		addFrame(getEffectHandler().getSequence().size());
	}

	@FXML
	void evtBack() {
		context.pop();
	}

	@FXML
	void evtColor() {
		if (!adjusting)
			setSelectedTo(color.valueProperty().get());
	}

	public ObjectProperty<KeyFrame> currentKeyFrame() {
		return currentKeyFrame;
	}

	public KeyFrame getCurrentKeyFrame() {
		return currentKeyFrame.get();
	}

	protected void setCurrentKeyFrame(KeyFrame selection) {
		this.currentKeyFrame.set(selection);
	}

	protected void setSelectedTo(Color col) {
		int[] rrgb = JavaFX.toRGB(col);
		KeyFrame kf = getSelectedFrame();
		for (MatrixIO el : MatrixView.expandMatrixElements(deviceViewer.getLayout(),
				deviceViewer.getSelectedElements())) {
			kf.getFrame()[el.getMatrixY()][el.getMatrixX()] = rrgb;
		}
		deviceViewer.updateFromMatrix(kf.getFrame());
		updateMatrix();
		updateAvailability();
	}

	@FXML
	void evtDefaultInterpolation() {
		getEffectHandler().getSequence().setInterpolation(defaultInterpolation.getValue());
	}

	@FXML
	void evtKeyFrameInterpolation() {
		getSelectedFrame().setInterpolation(keyFrameInterpolation.getValue());
	}

	@FXML
	void evtPause() {
		getEffectHandler().getPlayer().setPaused(!getEffectHandler().getPlayer().isPaused());
	}

	@FXML
	void evtPlay() {
		getEffectHandler().getPlayer().play();
	}

	@FXML
	void evtRed() {
		if (!adjusting)
			setSelectedTo(Color.RED);
	}

	@FXML
	void evtOrange() {
		if (!adjusting)
			setSelectedTo(Color.ORANGE);
	}

	@FXML
	void evtYellow() {
		if (!adjusting)
			setSelectedTo(Color.YELLOW);
	}

	@FXML
	void evtGreen() {
		if (!adjusting)
			setSelectedTo(Color.LIME);
	}

	@FXML
	void evtBlue() {
		if (!adjusting)
			setSelectedTo(Color.BLUE);
	}

	@FXML
	void evtIndigo() {
		if (!adjusting)
			setSelectedTo(Color.INDIGO);
	}

	@FXML
	void evtViolet() {
		if (!adjusting)
			setSelectedTo(Color.VIOLET);
	}

	@FXML
	void evtWhite() {
		if (!adjusting)
			setSelectedTo(Color.WHITE);
	}

	@FXML
	void evtRemoveFrame() {
		KeyFrame frame = getSelectedFrame();
		removeFrame(frame);
	}

	@FXML
	void evtRemoveEffect() {
		Confirm confirm = context.push(Confirm.class, Direction.FADE);
		confirm.confirm(bundle, "removeEffect", () -> {
			context.getEffectManager().remove(getEffectHandler());
			context.pop();
		}, getEffectHandler().getName());
	}

	@FXML
	void evtExport() {
		CustomEffectHandler effect = getEffectHandler();
		Sequence sequence = effect.getSequence();
		CustomEffect addOn = new CustomEffect(Strings.toId(effect.getName()));
		addOn.setName(effect.getName());
		addOn.setDescription(MessageFormat.format(bundle.getString("addOnTemplate.description"),
				sequence.getTotalFrames(), Time.formatTime(sequence.getTotalLength()), getDevice().getName()));
		addOn.setSequence(effect.getSequence());
		Export confirm = context.push(Export.class, Direction.FADE);
		confirm.export(addOn, bundle, "exportEffect", effect.getName());
	}

	@FXML
	void evtShiftLeft() {
		// TODO
	}

	@FXML
	void evtShiftRight() {
		// TODO
	}

	@FXML
	void evtShiftUp() {
		// TODO
	}

	@FXML
	void evtShiftDown() {
		// TODO
	}

	@FXML
	void evtRepeat() {
		getEffectHandler().getSequence().setRepeat(repeat.selectedProperty().get());
		rebuildTimeline();
		saveSequence();
	}

	@FXML
	public void evtReset() {
		KeyFrame f = getCurrentKeyFrame();
		List<IO> sel = deviceViewer.getSelectedElements();
		if (sel.isEmpty()) {
			deviceViewer.getSelectedView().getElements();
		}
		int[][][] frame = f.getFrame();
		for (IO io : sel) {
			if (io instanceof MatrixIO) {
				MatrixIO mio = (MatrixIO) io;
				frame[mio.getMatrixY()][mio.getMatrixX()] = Colors.COLOR_BLACK;
			}
		}
		deviceViewer.deselectAll();
		updateMatrix();
		setColorForButtons();
		updateAvailability();
	}

	@FXML
	void evtShowTimeline() {
		timelineHidden = false;
		PREFS.putBoolean(PREF_TIMELINE_VISIBLE, true);
		double pos = PREFS.getDouble(PREF_TIMELINE_DIVIDER, 0.75);
		Transition slideTransition = new Transition() {
			{
				setCycleDuration(Duration.millis(200));
			}

			@Override
			protected void interpolate(double frac) {
				double p = 1 - ((1 - pos) * frac);
				split.setDividerPositions(p);
			}
		};
		slideTransition.onFinishedProperty().set((e) -> timelineContainer.visibleProperty().set(true));
		slideTransition.setAutoReverse(false);
		slideTransition.setCycleCount(1);
		slideTransition.play();
	}

	@FXML
	void evtHideTimeline() {
		PREFS.putDouble(PREF_TIMELINE_DIVIDER, split.getDividerPositions()[0]);
		timelineContainer.visibleProperty().set(false);
		PREFS.putBoolean(PREF_TIMELINE_VISIBLE, false);
		double pos = split.getDividerPositions()[0];
		Transition slideTransition = new Transition() {
			{
				setCycleDuration(Duration.millis(200));
			}

			@Override
			protected void interpolate(double frac) {
				split.setDividerPositions(pos + ((1f - pos) * frac));
			}
		};
		slideTransition.onFinishedProperty().set((e) -> {
			Platform.runLater(() -> timelineHidden = true);
		});
		slideTransition.setAutoReverse(false);
		slideTransition.setCycleCount(1);
		slideTransition.play();
	}

	@FXML
	void evtSelectAll() {
		deviceViewer.selectAll();
		setColorForButtons();
		updateAvailability();
	}

	@FXML
	void evtStop() {
		FramePlayer player = getEffectHandler().getPlayer();
		if (player.isPlaying())
			player.stop();
	}

	void setColorForButtons() {
		adjusting = true;
		try {
			color.valueProperty().set(JavaFX.toColor(MatrixView.getRGBAverage(deviceViewer.getLayout(),
					deviceViewer.getSelectedElements(), getSelectedFrame().getFrame())));
		} finally {
			adjusting = false;
		}
	}

	BorderPane frameButton(KeyFrame frame, boolean last) {
		Canvas icon = buildFrameIcon(frame);
		Hyperlink button = new Hyperlink();
		button.onActionProperty().set((e) -> {
			selectFrame(frame);
		});
		button.setAlignment(Pos.CENTER);
		button.setGraphic(icon);
		ContextMenu menu = creatContextMenu(frame, button);
		button.contextMenuProperty().set(menu);

		Spinner<Double> holdFor = new Spinner<>(0, 9999, (double) frame.getHoldFor() / 1000.0, 1);
		holdFor.promptTextProperty().set(null);
		holdFor.getStyleClass().add("small");
		holdFor.editableProperty().set(true);
		holdFor.prefWidth(100);
		holdFor.valueProperty().addListener((e) -> {
			frame.setHoldFor((long) ((double) holdFor.valueProperty().get() * 1000.0));
			rebuildTimestats();
		});

		Label arrow = new Label(last ? (getEffectHandler().getSequence().isRepeat() ? "\uf01e" : "\uf28d") : "\uf061");
		arrow.getStyleClass().add("icon");
		BorderPane.setAlignment(arrow, Pos.CENTER);

		BorderPane h = new BorderPane();
		h.setCenter(button);
//		h.setBottom(holdFor);
		h.setRight(arrow);

		return h;
	}

	KeyFrame getSelectedFrame() {
		return getEffectHandler().getSequence().getFrameAt((long) (progress.valueProperty().doubleValue() * 1000.0));
	}

	void rebuildTimeline() {
		timeline.getChildren().clear();
		keyFrames.clear();
		Sequence seq = getEffectHandler().getSequence();
		for (int i = 0; i < seq.size(); i++) {
			KeyFrame keyFrame = seq.get(i);
			BorderPane fb = frameButton(keyFrame, i == seq.size() - 1);
			keyFrames.put(keyFrame, fb);
			timeline.getChildren().add(fb);
		}
		rebuildTimestats();
		timeline.getChildren().get(0).requestFocus();
	}

	void rebuildTimestats() {
		CustomEffectHandler fx = getEffectHandler();
		Sequence seq = fx.getSequence();
		FramePlayer player = fx.getPlayer();
		long totalLength = seq.getTotalLength();
		long totalFrames = seq.getTotalFrames();

		long frameNumber;
		long frameTime;
		if (player.isActive()) {
			frameNumber = player.getFrameNumber();
			frameTime = player.getTimeElapsed();

			if (!adjustingProgress) {
				try {
					adjustingProgress = true;
					progress.valueProperty().set((double) player.getTimeElapsed() / 1000.0);
				} finally {
					adjustingProgress = false;
				}
			}

			KeyFrame f = player.getFrame();
			if (!Objects.equals(playingFrame, f)) {
				playingFrame = f;
				updateKeyFrameConfiguration(playingFrame);
				for (Map.Entry<KeyFrame, BorderPane> bp : keyFrames.entrySet()) {
					if (bp.getKey().equals(playingFrame)) {
						double x = bp.getValue().layoutXProperty().get();
						double scrollWidth = Math.max(timelineScrollPane.getViewportBounds().getWidth(),
								timeline.prefWidth(100) - timelineScrollPane.getViewportBounds().getWidth());
						double newPos = x / scrollWidth;
						timelineScrollPane.setHvalue(newPos);
						bp.getValue().setEffect(new Glow(0.8));
					} else
						bp.getValue().setEffect(null);
				}
			}
		} else {
			if (playingFrame != null) {
				timelineScrollPane.setHvalue(0);
				for (BorderPane bp : keyFrames.values())
					bp.setEffect(null);
				playingFrame = null;
			}
			frameTime = (long) (progress.valueProperty().doubleValue() * 1000.0);
			KeyFrame frameAt = seq.getFrameAt(frameTime);
			frameNumber = frameAt.getStartFrame();
		}
		frames.textProperty().set(MessageFormat.format(bundle.getString("frames"), frameNumber, totalFrames));
		time.textProperty().set(MessageFormat.format(bundle.getString("time"), Time.formatTime(frameTime),
				Time.formatTime(totalLength)));

		progress.maxProperty().set((double) totalLength / 1000.0);

	}

	void updateFrameInTimeline() {
		KeyFrame f = getSelectedFrame();
		BorderPane bp = keyFrames.get(f);
		Hyperlink c = (Hyperlink) bp.getCenter();
		c.setGraphic(buildFrameIcon(f));
	}

	void updateMatrix() {
//		int[][][] rgb = new int[deviceY][deviceX][3];
//		for (Cell cell : this.buttons.keySet()) {
//			rgb[cell.getY()][cell.getX()] = buttons.get(cell).getRgb();
//		}
//		getSelectedFrame().setFrame(rgb);
//		Matrix matrix = new Matrix();
//		matrix.setCells(rgb);
//		context.getScheduler().execute(() -> getDevice().updateEffect(matrix));
//		updateFrameInTimeline();
//		saveSequence();

		updateFrameInTimeline();
		saveSequence();
		getEffectHandler().update(getDevice());
	}

	private void updateAvailability() {
		FramePlayer player = getEffectHandler().getPlayer();
		play.disableProperty().set(player.isPlaying());
		stop.disableProperty().set(!player.isPlaying());
		pause.disableProperty().set(!player.isPlaying());
		progress.disableProperty().set(player.isPlaying() && !player.isPaused());
//		for (Map.Entry<Cell, MatrixCellButton> en : buttons.entrySet()) {
//			en.getValue().disableProperty().set(player.isPaused());
//		}
		progress.onMouseReleasedProperty().set((e) -> {
			if (!player.isPlaying()) {
				KeyFrame frame = getEffectHandler().getSequence()
						.getFrameAt((long) (progress.valueProperty().get() * 1000.0));
				progress.valueProperty().set((double) frame.getIndex() / 1000.0);
			}
		});
		addFrame.disableProperty().set(player.isPlaying());
		removeFrame.disableProperty().set(player.isPlaying() || player.getSequence().size() < 2);
		keyFrameInterpolation.disableProperty().set(player.isPlaying());
		holdKeyFrameFor.disableProperty().set(player.isPlaying());
		reset.disableProperty().set(player.isPlaying());
		selectAll.disableProperty().set(player.isPlaying());
		removeEffect.disableProperty().set(player.isPlaying());
//		List<ToggleButton> selectedButtons = getSelectedButtons();
//		shiftLeft.disableProperty().set(player.isPlaying() || selectedButtons.size() == 0);
//		shiftRight.disableProperty().set(player.isPlaying() || selectedButtons.size() == 0);
//		shiftUp.disableProperty().set(player.isPlaying() || selectedButtons.size() == 0);
//		shiftDown.disableProperty().set(player.isPlaying() || selectedButtons.size() == 0);
	}

}
