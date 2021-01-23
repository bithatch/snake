package uk.co.bithatch.snake.ui;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.prefs.Preferences;

import javafx.animation.Transition;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.effect.Glow;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.animation.AudioDataProvider;
import uk.co.bithatch.snake.lib.animation.AudioParameters;
import uk.co.bithatch.snake.lib.animation.FramePlayer;
import uk.co.bithatch.snake.lib.animation.FramePlayer.FrameListener;
import uk.co.bithatch.snake.lib.animation.Interpolation;
import uk.co.bithatch.snake.lib.animation.KeyFrame;
import uk.co.bithatch.snake.lib.animation.KeyFrame.KeyFrameCellSource;
import uk.co.bithatch.snake.lib.animation.KeyFrameCell;
import uk.co.bithatch.snake.lib.animation.Sequence;
import uk.co.bithatch.snake.lib.layouts.Area;
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.DeviceLayout;
import uk.co.bithatch.snake.lib.layouts.DeviceView;
import uk.co.bithatch.snake.lib.layouts.IO;
import uk.co.bithatch.snake.lib.layouts.MatrixCell;
import uk.co.bithatch.snake.lib.layouts.MatrixIO;
import uk.co.bithatch.snake.lib.layouts.ViewPosition;
import uk.co.bithatch.snake.ui.addons.CustomEffect;
import uk.co.bithatch.snake.ui.designer.MatrixView;
import uk.co.bithatch.snake.ui.designer.TabbedViewer;
import uk.co.bithatch.snake.ui.effects.CustomEffectHandler;
import uk.co.bithatch.snake.ui.util.Strings;
import uk.co.bithatch.snake.ui.util.Time;
import uk.co.bithatch.snake.widgets.ColorBar;
import uk.co.bithatch.snake.widgets.Direction;
import uk.co.bithatch.snake.widgets.GeneratedIcon;
import uk.co.bithatch.snake.widgets.JavaFX;

public class CustomOptions extends AbstractEffectController<Sequence, CustomEffectHandler> implements FrameListener {

	final static ResourceBundle bundle = ResourceBundle.getBundle(CustomOptions.class.getName());

	final static String PREF_TIMELINE_DIVIDER = "timelineDivider";
	final static String PREF_TIMELINE_VISIBLE = "timelineVisible";
	final static Preferences PREFS = Preferences.userNodeForPackage(CustomOptions.class);

	private static final double MIN_SPEED = 0.001;

	final static int[] getRGBAverage(DeviceLayout layout, Collection<IO> elements, KeyFrame frame,
			AudioDataProvider audio) {
		DeviceView matrixView = null;
		int[] rgb = new int[3];
		int r = 0;
		for (IO element : elements) {
			if (element instanceof Area) {
				if (matrixView == null)
					matrixView = layout.getViews().get(ViewPosition.MATRIX);
				Area area = (Area) element;
				Region.Name region = area.getRegion();
				for (IO cell : matrixView.getElements()) {
					MatrixCell mc = (MatrixCell) cell;
					if (mc.getRegion() == region) {
						int[] rr = frame.getCell(mc.getMatrixX(), mc.getMatrixY()).getValues();
						rgb[0] += rr[0];
						rgb[1] += rr[1];
						rgb[2] += rr[2];
						r++;

					}
				}
			} else if (element instanceof MatrixIO) {
				MatrixIO matrixIO = (MatrixIO) element;
				if (matrixIO.isMatrixLED()) {
					int[] rr = frame.getCell(matrixIO.getMatrixX(), matrixIO.getMatrixY()).getValues();
					rgb[0] += rr[0];
					rgb[1] += rr[1];
					rgb[2] += rr[2];
					r++;
				}
			}
		}
		if (r == 0)
			return Colors.COLOR_BLACK;
		else
			return new int[] { rgb[0] / r, rgb[1] / r, rgb[2] / r };
	}

	@FXML
	private Hyperlink addFrame;
	@FXML
	private Tab animation;
	@FXML
	private ComboBox<KeyFrameCellSource> cellBrightness;
	@FXML
	private ComboBox<KeyFrameCellSource> cellHue;
	@FXML
	private ComboBox<Interpolation> cellInterpolation;
	@FXML
	private ComboBox<KeyFrameCellSource> cellSaturation;
	@FXML
	private ColorBar colorBar;
	@FXML
	private BorderPane container;
	@FXML
	private TabPane customEditorTabs;
	@FXML
	private ComboBox<Interpolation> defaultInterpolation;
	@FXML
	private Label effectName;
	@FXML
	private Hyperlink export;
	@FXML
	private Spinner<Integer> fps;
	@FXML
	private Label frames;
	@FXML
	private Spinner<Double> gain;
	@FXML
	private Hyperlink hideTimeline;
	@FXML
	private Spinner<Integer> high;
	@FXML
	private Spinner<Double> holdKeyFrameFor;
	@FXML
	private ComboBox<Interpolation> keyFrameInterpolation;
	@FXML
	private Spinner<Integer> keyFrameNumber;
	@FXML
	private Tab keyFrameOptions;
	@FXML
	private Spinner<Integer> low;
	@FXML
	private Hyperlink pause;
	@FXML
	private Hyperlink play;
	@FXML
	private Slider progress;
	@FXML
	private Tab properties;
	@FXML
	private Hyperlink removeEffect;
	@FXML
	private Hyperlink removeFrame;
	@FXML
	private CheckBox repeat;
	@FXML
	private Button shiftDown;
	@FXML
	private Button shiftLeft;
	@FXML
	private Button shiftRight;
	@FXML
	private Button shiftUp;
	@FXML
	private Hyperlink showTimeline;
	@FXML
	private Spinner<Double> speed;
	@FXML
	private SplitPane split;
	@FXML
	private Hyperlink stop;
	@FXML
	private Label time;
	@FXML
	private HBox timeline;
	@FXML
	private BorderPane timelineContainer;
	@FXML
	private ScrollPane timelineScrollPane;

	private TabbedViewer deviceViewer;
	private int deviceX;
	private int deviceY;
	private boolean keyFrameAdjusting;
	private IntegerSpinnerValueFactory keyFrameNumberFactory;
	private Map<KeyFrame, BorderPane> keyFrames = new HashMap<>();
	private KeyFrame playingFrame;
	private boolean timelineHidden;
	private boolean adjusting;
	private boolean adjustingProgress;
	private ObjectProperty<KeyFrame> currentKeyFrame = new SimpleObjectProperty<>(null, "currentKeyFrame");

	public ObjectProperty<KeyFrame> currentKeyFrame() {
		return currentKeyFrame;
	}

	@FXML
	public void evtReset() {
		KeyFrame f = getCurrentKeyFrame();
		List<IO> sel = deviceViewer.getSelectedElements();
		if (sel.isEmpty()) {
			deviceViewer.getSelectedView().getElements();
		}
		for (IO io : sel) {
			if (io instanceof MatrixIO) {
				MatrixIO mio = (MatrixIO) io;
				f.setCell(mio.getMatrixX(), mio.getMatrixY(), new KeyFrameCell(Colors.COLOR_BLACK));
			}
		}
		deviceViewer.deselectAll();
		updateState();
	}

	@Override
	public void frameUpdate(KeyFrame frame, int[][][] rgb, float fac, long frameNumber) {
		Platform.runLater(() -> {
			updateFrame(rgb);
			rebuildTimestats();
		});
	}

	public KeyFrame getCurrentKeyFrame() {
		return currentKeyFrame.get();
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

	protected Set<Integer> getSelectedMatrixRows() {
		Set<Integer> rows = new HashSet<>();
		for (MatrixIO el : MatrixView.expandMatrixElements(deviceViewer.getLayout(),
				deviceViewer.getSelectedElements())) {
			rows.add(el.getMatrixY());
		}
		return rows;
	}

	protected Set<Integer> getSelectedMatrixColumns() {
		Set<Integer> rows = new HashSet<>();
		for (MatrixIO el : MatrixView.expandMatrixElements(deviceViewer.getLayout(),
				deviceViewer.getSelectedElements())) {
			rows.add(el.getMatrixX());
		}
		return rows;
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

		for (Interpolation ip : Interpolation.interpolations()) {
			if (!ip.equals(Interpolation.sequence) && !ip.equals(Interpolation.keyframe))
				defaultInterpolation.itemsProperty().get().add(ip);
			if (!ip.equals(Interpolation.keyframe))
				keyFrameInterpolation.itemsProperty().get().add(ip);
			if (!ip.equals(Interpolation.sequence))
				cellInterpolation.itemsProperty().get().add(ip);
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
				updateSelection();
				updateAvailability();
			}
		});

		timelineContainer.managedProperty().bind(timelineContainer.visibleProperty());
		hideTimeline.managedProperty().bind(hideTimeline.visibleProperty());
		hideTimeline.visibleProperty().bind(timelineContainer.visibleProperty());
		showTimeline.managedProperty().bind(showTimeline.visibleProperty());
		showTimeline.visibleProperty().bind(Bindings.not(timelineContainer.visibleProperty()));

		cellHue.getItems().addAll(KeyFrameCellSource.values());
		cellSaturation.getItems().addAll(KeyFrameCellSource.values());
		cellBrightness.getItems().addAll(KeyFrameCellSource.values());

		cellHue.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				setSelectedTo(colorBar.getColor(), n, 0);
			}
		});
		cellSaturation.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting)
				setSelectedTo(colorBar.getColor(), n, 1);
		});
		cellBrightness.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting)
				setSelectedTo(colorBar.getColor(), n, 2);
		});
	}

	@Override
	protected void onSetEffectHandler() {
		CustomEffectHandler effect = getEffectHandler();
		FramePlayer player = effect.getPlayer();
		deviceViewer.setSelectableElements(!player.isActive());

		effectName.textProperty().set(effect.getDisplayName());
		keyFrameNumberFactory = new SpinnerValueFactory.IntegerSpinnerValueFactory(0,
				Math.max(0, effect.getSequence().size() - 1), 1, 1);
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
		low.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 255,
				effect.getSequence().getAudioParameters() == null ? 0
						: effect.getSequence().getAudioParameters().getLow(),
				1));
		low.valueProperty().addListener((e, o, n) -> {
			if (!adjusting)
				setSelectedAudioLow(n);
		});
		high.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 255,
				effect.getSequence().getAudioParameters() == null ? 255
						: effect.getSequence().getAudioParameters().getHigh(),
				1));
		high.valueProperty().addListener((e, o, n) -> {
			if (!adjusting)
				setSelectedAudioHigh(n);
		});
		gain.setValueFactory(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 20,
				effect.getSequence().getAudioParameters() == null ? 1.0
						: effect.getSequence().getAudioParameters().getGain(),
				0.1));
		gain.valueProperty().addListener((e, o, n) -> {
			if (!adjusting)
				setSelectedAudioGain(n.floatValue());
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
			updateSelection();
		});
		colorBar.colorProperty().addListener((e, o, n) -> {
			if (!adjusting)
				setSelectedTo(n);
		});
		cellInterpolation.getSelectionModel().selectedItemProperty()
				.addListener((e, o, n) -> setSelectedToInterpolation(n));

		rebuildTimeline();
		updateAvailability();
		rebuildTimestats();

		player.addListener(this);

		double pos = PREFS.getDouble(PREF_TIMELINE_DIVIDER, -1);
		boolean vis = PREFS.getBoolean(PREF_TIMELINE_VISIBLE, true);
		split.getDividers().get(0).positionProperty().addListener((e, o, n) -> {
			if (timelineContainer.visibleProperty().get()) {
				PREFS.putDouble(PREF_TIMELINE_DIVIDER, split.getDividerPositions()[0]);
			} else if (timelineHidden && n.floatValue() > 1) {
				evtShowTimeline();
			}
		});
		if (vis) {
			split.setDividerPositions(pos == -1 ? 0.75 : pos);
		} else {
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
						updateFrame(f.getRGBFrame(context.getAudioManager()));
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

	protected void setCurrentKeyFrame(KeyFrame selection) {
		this.currentKeyFrame.set(selection);
	}

	protected void setSelectedAudioGain(float gain) {
		Sequence seq = getEffectHandler().getSequence();
		KeyFrame kf = getSelectedFrame();
		AudioParameters audio = seq.getAudioParameters();
		if (audio != null && audio.getLow() == 0 && audio.getHigh() == 255 && gain == 1) {
			/* So we don't export default parameters */
			seq.setAudioParameters(null);
		} else if (gain != 1) {
			if (audio == null) {
				audio = new AudioParameters();
				seq.setAudioParameters(audio);
			}
			audio.setGain(gain);
		}
		deviceViewer.updateFromMatrix(kf.getRGBFrame(context.getAudioManager()));
		updateState();
	}

	protected void setSelectedAudioHigh(int high) {
		Sequence seq = getEffectHandler().getSequence();
		KeyFrame kf = getSelectedFrame();
		AudioParameters audio = seq.getAudioParameters();
		if (audio != null && audio.getLow() == 0 && audio.getGain() == 1 && high == 255) {
			/* So we don't export default parameters */
			seq.setAudioParameters(null);
		} else if (high != 255) {
			if (audio == null) {
				audio = new AudioParameters();
				seq.setAudioParameters(audio);
			}
			audio.setHigh(high);
		}
		deviceViewer.updateFromMatrix(kf.getRGBFrame(context.getAudioManager()));
		updateState();
	}

	protected void setSelectedAudioLow(int low) {
		Sequence seq = getEffectHandler().getSequence();
		KeyFrame kf = getSelectedFrame();
		AudioParameters audio = seq.getAudioParameters();
		if (audio != null && audio.getHigh() == 255 && audio.getGain() == 1 && low == 0) {
			/* So we don't export default parameters */
			seq.setAudioParameters(null);
		} else if (low != 0) {
			if (audio == null) {
				audio = new AudioParameters();
				seq.setAudioParameters(audio);
			}
			audio.setLow(low);
		}
		deviceViewer.updateFromMatrix(kf.getRGBFrame(context.getAudioManager()));
		updateState();
	}

	protected void setSelectedTo(Color col) {
		int[] rrgb = JavaFX.toRGB(col);
		KeyFrame kf = getSelectedFrame();
		for (MatrixIO el : MatrixView.expandMatrixElements(deviceViewer.getLayout(),
				deviceViewer.getSelectedElements())) {
			kf.setRGB(el.getMatrixX(), el.getMatrixY(), rrgb);
		}
		colorBar.setColor(col);
		deviceViewer.updateFromMatrix(kf.getRGBFrame(context.getAudioManager()));
		updateState();
	}

	protected void setSelectedTo(Color col, KeyFrameCellSource source, int index) {
		KeyFrame kf = getSelectedFrame();
		for (MatrixIO el : MatrixView.expandMatrixElements(deviceViewer.getLayout(),
				deviceViewer.getSelectedElements())) {
			KeyFrameCell kfc = kf.getCell(el.getMatrixX(), el.getMatrixY());
			kfc.getSources()[index] = source;
			kfc.setValues(JavaFX.toRGB(col));
		}
		deviceViewer.updateFromMatrix(kf.getRGBFrame(context.getAudioManager()));
		updateState();
	}

	protected void setSelectedToInterpolation(Interpolation n) {
		if (n != null) {
			KeyFrame kf = getSelectedFrame();
			for (MatrixIO el : MatrixView.expandMatrixElements(deviceViewer.getLayout(),
					deviceViewer.getSelectedElements())) {
				kf.getCell(el.getMatrixX(), el.getMatrixY()).setInterpolation(n);
			}
			deviceViewer.updateFromMatrix(kf.getRGBFrame(context.getAudioManager()));
			updateState();
		}
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

	protected void updateState() {
		updateMatrix();
		updateAvailability();
		updateSelection();
	}

	GeneratedIcon buildFrameIcon(KeyFrame frame) {
		GeneratedIcon gi = new GeneratedIcon();
		gi.setPrefHeight(64);
		gi.setPrefWidth(64);
		int[] rgb = frame.getOverallColor(context.getAudioManager());
		gi.getStyleClass().add("keyframe-button");
		gi.setStyle("-icon-color: " + Colors.toHex(rgb));
		gi.setText(Time.formatTime(frame.getIndex()));
		gi.setFontSize(10);
		return gi;

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
	void evtDefaultInterpolation() {
		getEffectHandler().getSequence().setInterpolation(defaultInterpolation.getValue());
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
	void evtKeyFrameInterpolation() {
		getSelectedFrame().setInterpolation(keyFrameInterpolation.getValue());
	}

	@FXML
	void evtNone() {
		if (!adjusting)
			setSelectedTo(Color.BLACK);
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
	void evtRemoveEffect() {
		Confirm confirm = context.push(Confirm.class, Direction.FADE);
		confirm.confirm(bundle, "removeEffect", () -> {
			context.getEffectManager().remove(getEffectHandler());
			context.pop();
		}, getEffectHandler().getName());
	}

	@FXML
	void evtRemoveFrame() {
		KeyFrame frame = getSelectedFrame();
		removeFrame(frame);
	}

	@FXML
	void evtRepeat() {
		getEffectHandler().getSequence().setRepeat(repeat.selectedProperty().get());
		rebuildTimeline();
		saveSequence();
	}

	@FXML
	void evtSelectAll() {
		deviceViewer.selectAll();
		updateSelection();
		updateAvailability();
	}

	@FXML
	void evtShiftDown() {
		KeyFrame f = getCurrentKeyFrame();
		for (Integer x : getSelectedMatrixRows()) {
			KeyFrameCell endCell = null, cell = null, otherCell = null;
			for (int y = deviceY - 1; y >= 0; y--) {
				cell = f.getCell(x, y);
				if (endCell == null) {
					endCell = new KeyFrameCell(cell);
				}
				otherCell = f.getCell(x - 1, y);
				cell.copyFrom(otherCell);
				f.setCell(x, y, cell);
			}
			cell = f.getCell(x, 0);
			cell.copyFrom(endCell);
			f.setCell(x, 0, cell);
		}
		deviceViewer.updateFromMatrix(f.getRGBFrame(context.getAudioManager()));
		updateState();
	}

	@FXML
	void evtShiftLeft() {
		KeyFrame f = getCurrentKeyFrame();
		for (Integer y : getSelectedMatrixRows()) {
			KeyFrameCell endCell = null, cell = null, otherCell = null;
			for (int x = 0; x < deviceX - 1; x++) {
				cell = f.getCell(x, y);
				if (endCell == null) {
					endCell = new KeyFrameCell(cell);
				}
				otherCell = f.getCell(x + 1, y);
				cell.copyFrom(otherCell);
				f.setCell(x, y, cell);
			}
			cell = f.getCell(deviceX - 1, y);
			cell.copyFrom(endCell);
			f.setCell(deviceX - 1, y, cell);
		}
		deviceViewer.updateFromMatrix(f.getRGBFrame(context.getAudioManager()));
		updateState();
	}

	@FXML
	void evtShiftRight() {
		KeyFrame f = getCurrentKeyFrame();
		for (Integer y : getSelectedMatrixRows()) {
			KeyFrameCell endCell = null, cell = null, otherCell = null;
			for (int x = deviceX - 1; x > 0; x--) {
				cell = f.getCell(x, y);
				if (endCell == null) {
					endCell = new KeyFrameCell(cell);
				}
				otherCell = f.getCell(x - 1, y);
				cell.copyFrom(otherCell);
				f.setCell(x, y, cell);
			}
			cell = f.getCell(0, y);
			cell.copyFrom(endCell);
			f.setCell(0, y, cell);
		}
		deviceViewer.updateFromMatrix(f.getRGBFrame(context.getAudioManager()));
		updateState();
	}

	@FXML
	void evtShiftUp() {
		KeyFrame f = getCurrentKeyFrame();
		for (Integer x : getSelectedMatrixRows()) {
			KeyFrameCell endCell = null, cell = null, otherCell = null;
			for (int y = 0; y < deviceY - 1; y++) {
				cell = f.getCell(x, y);
				if (endCell == null) {
					endCell = new KeyFrameCell(cell);
				}
				otherCell = f.getCell(x + 1, y);
				cell.copyFrom(otherCell);
				f.setCell(x, y, cell);
			}
			cell = f.getCell(x, deviceY - 1);
			cell.copyFrom(endCell);
			f.setCell(x, deviceY - 1, cell);
		}
		deviceViewer.updateFromMatrix(f.getRGBFrame(context.getAudioManager()));
		updateState();
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
	void evtStop() {
		FramePlayer player = getEffectHandler().getPlayer();
		if (player.isPlaying()) {
			player.stop();
		}
	}

	BorderPane frameButton(KeyFrame frame, boolean last) {
		Node icon = buildFrameIcon(frame);
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
		updateFrameInTimeline();
		saveSequence();
		getEffectHandler().update(getDevice());
	}

	void updateSelection() {
		adjusting = true;
		try {
			List<IO> sel = deviceViewer.getSelectedElements();
			if (sel.isEmpty()) {
				cellInterpolation.setDisable(true);
				cellHue.setDisable(true);
				cellSaturation.setDisable(true);
				cellBrightness.setDisable(true);
				colorBar.disableProperty().set(true);
			} else {
				cellInterpolation.setDisable(false);
				cellHue.setDisable(false);
				cellSaturation.setDisable(false);
				cellBrightness.setDisable(false);
				KeyFrame frame = getSelectedFrame();

				Color col = JavaFX
						.toColor(getRGBAverage(deviceViewer.getLayout(), sel, frame, context.getAudioManager()));

				KeyFrameCellSource thisHueSrc = null;
				KeyFrameCellSource thisSaturationSrc = null;
				KeyFrameCellSource thisBrightnessSrc = null;
				Interpolation thisInterpol = null;
				for (IO io : sel) {
					MatrixIO mio = (MatrixIO) io;
					KeyFrameCell keyFrameCell = frame.getCell(mio.getMatrixX(), mio.getMatrixY());
					if (thisHueSrc == null || thisHueSrc != keyFrameCell.getSources()[0])
						thisHueSrc = keyFrameCell.getSources()[0];
					if (thisSaturationSrc == null || thisSaturationSrc != keyFrameCell.getSources()[1])
						thisSaturationSrc = keyFrameCell.getSources()[1];
					if (thisBrightnessSrc == null || thisBrightnessSrc != keyFrameCell.getSources()[2])
						thisBrightnessSrc = keyFrameCell.getSources()[2];
					if (thisInterpol == null || thisInterpol != keyFrameCell.getInterpolation())
						thisInterpol = keyFrameCell.getInterpolation();
				}
				colorBar.disableProperty()
						.set(thisHueSrc != KeyFrameCellSource.COLOR && thisSaturationSrc != KeyFrameCellSource.COLOR
								&& thisBrightnessSrc != KeyFrameCellSource.COLOR);
				cellHue.getSelectionModel().select(thisHueSrc);
				cellSaturation.getSelectionModel().select(thisSaturationSrc);
				cellBrightness.getSelectionModel().select(thisBrightnessSrc);
				cellInterpolation.getSelectionModel().select(thisInterpol);
				colorBar.setColor(col);
			}
		} finally {
			adjusting = false;
		}
	}

	private void updateAvailability() {
		FramePlayer player = getEffectHandler().getPlayer();
		play.disableProperty().set(player.isPlaying());
		stop.disableProperty().set(!player.isPlaying());
		pause.disableProperty().set(!player.isPlaying());
		progress.disableProperty().set(player.isPlaying() && !player.isPaused());
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
		removeEffect.disableProperty().set(player.isPlaying());
		List<IO> sel = deviceViewer.getSelectedElements();
		shiftLeft.disableProperty().set(player.isPlaying() || sel.size() == 0 || deviceX < 2);
		shiftRight.disableProperty().set(player.isPlaying() || sel.size() == 0 || deviceX < 2);
		shiftUp.disableProperty().set(player.isPlaying() || sel.size() == 0 || deviceY < 2);
		shiftDown.disableProperty().set(player.isPlaying() || sel.size() == 0 || deviceY < 2);
	}

}
