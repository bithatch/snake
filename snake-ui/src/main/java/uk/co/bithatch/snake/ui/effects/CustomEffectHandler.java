package uk.co.bithatch.snake.ui.effects;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import com.sshtools.icongenerator.IconBuilder.TextContent;

import javafx.scene.Node;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.animation.AudioParameters;
import uk.co.bithatch.snake.lib.animation.FramePlayer;
import uk.co.bithatch.snake.lib.animation.Interpolation;
import uk.co.bithatch.snake.lib.animation.KeyFrame;
import uk.co.bithatch.snake.lib.animation.KeyFrame.KeyFrameCellSource;
import uk.co.bithatch.snake.lib.animation.KeyFrameCell;
import uk.co.bithatch.snake.lib.animation.Sequence;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.lib.effects.Matrix;
import uk.co.bithatch.snake.ui.CustomOptions;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;
import uk.co.bithatch.snake.ui.util.Prefs;
import uk.co.bithatch.snake.ui.util.Strings;
import uk.co.bithatch.snake.widgets.GeneratedIcon;

public class CustomEffectHandler extends AbstractEffectHandler<Sequence, CustomOptions> {

	private static final String PREF_HOLD_FOR = "holdFor";
	private static final String PREF_FPX = "fps";
	private static final String PREF_FRAMES = "frames";
	private static final String PREF_INTERPOLATION = "interpolation";
	private static final String PREF_REPEAT = "repeat";
	private static final String PREF_SPEED = "speed";

	private String name;
	private FramePlayer player;
	private Sequence sequence;
	private boolean readOnly;

	public CustomEffectHandler() {
		this(null);
	}

	public CustomEffectHandler(String name) {
		this(name, null);
	}

	public CustomEffectHandler(String name, Sequence sequence) {
		this.name = name;
		if (sequence == null) {
			sequence = new Sequence();

			sequence.setInterpolation(Interpolation.linear);
			sequence.setSpeed(1);
			sequence.setFps(25);
			sequence.setRepeat(true);
			sequence.add(new KeyFrame());
		}
		this.sequence = sequence;
	}

	@Override
	public Class<? extends Effect> getBackendEffectClass() {
		return Matrix.class;
	}

	public boolean isReadOnly() {
		return readOnly;
	}

	public void setReadOnly(boolean readOnly) {
		this.readOnly = readOnly;
	}

	public void setSequence(Sequence sequence) {
		this.sequence = sequence;
	}

	@Override
	public void added(Device device) {
		if (!isReadOnly())
			Prefs.addToStringSet(getContext().getEffectManager().getPreferences(device),
					EffectManager.PREF_CUSTOM_EFFECTS, getName());
	}

	@Override
	public void removed(Device device) {
		if (getPlayer().isPlaying())
			getPlayer().stop();
		if (!isReadOnly()) {
			Prefs.removeFromStringSet(getContext().getEffectManager().getPreferences(device),
					EffectManager.PREF_CUSTOM_EFFECTS, getName());
			try {
				getEffectPreferences(device).removeNode();
			} catch (BackingStoreException e) {
				throw new IllegalStateException("Failed to remove effect.", e);
			}
		}
	}

	@Override
	public String getDisplayName() {
		return name == null || name.length() == 0 ? bundle.getString("effect.Custom") : name;
	}

	@Override
	public URL getEffectImage(int size) {
		return getContext().getConfiguration().getTheme().getEffectImage(size, Matrix.class);
	}

	public Node getEffectImageNode(int size, int viewSize) {
		GeneratedIcon ib = new GeneratedIcon();
		ib.setPrefWidth(viewSize);
		ib.setPrefHeight(viewSize);
		ib.setMinWidth(viewSize);
		ib.setMinHeight(viewSize);
		ib.setMaxWidth(viewSize);
		ib.setMaxHeight(viewSize);
		ib.setText(getDisplayName());
		ib.setTextContent(TextContent.INITIALS);
		return ib;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Class<CustomOptions> getOptionsController() {
		return CustomOptions.class;
	}

	public FramePlayer getPlayer() {
		return player;
	}

	public Sequence getSequence() {
		return sequence;
	}

	@Override
	public boolean isSupported(Lit component) {
		return component == null ? false : component.getSupportedEffects().contains(Matrix.class);
	}

	public void setName(String name) {
		this.name = name;
	}

	protected void addDefaultFrames(Lit component) {
		Device dev = Lit.getDevice(component);
		int[] dim = dev.getMatrixSize();

		KeyFrame f1 = new KeyFrame();
		f1.setColor(new int[] { 0xff, 0x00, 0x00 }, dim[0], dim[1]);
		f1.setHoldFor(TimeUnit.SECONDS.toMillis(5));
		sequence.add(f1);

		KeyFrame f2 = new KeyFrame();
		f2.setColor(new int[] { 0x00, 0xff, 0x00 }, dim[0], dim[1]);
		f2.setHoldFor(TimeUnit.SECONDS.toMillis(5));
		sequence.add(f2);

		KeyFrame f3 = new KeyFrame();
		f3.setColor(new int[] { 0x00, 0x00, 0xff }, dim[0], dim[1]);
		f3.setHoldFor(TimeUnit.SECONDS.toMillis(5));
		sequence.add(f3);
	}

	@Override
	public void deactivate(Lit component) {
		if (player.isPlaying())
			player.stop();
	}

	@Override
	public void update(Lit component) {
		Matrix effect = (Matrix) component.getEffect();
		component.updateEffect(effect);
	}

	@Override
	protected Sequence onActivate(Lit component) {
		if (!player.isReady()) {
			player.setDevice(Lit.getDevice(component));
			player.setSequence(getSequence());
			Matrix effect = component.createEffect(Matrix.class);
			onLoad(component, getEffectPreferences(component), effect);
			player.setEffect(effect);
		}
		if (player.isPaused())
			player.setPaused(false);
		else if (!player.isPlaying())
			player.play();

		return getSequence();
	}

	protected void onLoad(Lit component, Preferences matrix, Matrix effect) {
		Preferences framesPrefs = matrix.node(PREF_FRAMES);
		sequence.setFps(matrix.getInt(PREF_FPX, 25));
		sequence.setRepeat(matrix.getBoolean(PREF_REPEAT, true));
		sequence.setSpeed(matrix.getFloat(PREF_SPEED, 1));
		int low = matrix.getInt("audioLow", 0);
		int high = matrix.getInt("audioHigh", 255);
		float gain = matrix.getFloat("audioGain", 1f);
		if (low != 0 || high != 255 || gain != 1) {
			AudioParameters parameters = new AudioParameters();
			parameters.setLow(low);
			parameters.setHigh(high);
			parameters.setGain(gain);
			sequence.setAudioParameters(parameters);
		}
		sequence.setInterpolation(Interpolation.get(matrix.get(PREF_INTERPOLATION, Interpolation.linear.getName())));
		sequence.clear();
		try {
			String[] childrenNames = framesPrefs.childrenNames();
			for (String frameId : childrenNames) {
				Preferences frameNode = framesPrefs.node(frameId);
				KeyFrame frame = load(frameNode);
				sequence.add(frame);
			}
		} catch (BackingStoreException bse) {
			throw new IllegalStateException("Could not load matrix effect configuration.", bse);
		}

		/* Must have one frame */
		if (sequence.isEmpty()) {
			addDefaultFrames(component);
			if (!isReadOnly())
				onSave(matrix);
		}
	}

	protected void onSave(Preferences prefs) {
		if (readOnly)
			throw new IllegalStateException("This is a read only effect.");

		/* Delete existing matrix */
		Preferences customNode = prefs.node(PREF_FRAMES);
		try {
			customNode.removeNode();
			customNode = prefs.node(PREF_FRAMES);
		} catch (BackingStoreException bse) {
			throw new IllegalStateException("Failed to remove old custom.", bse);
		}

		int id = 1;
		prefs.put(PREF_INTERPOLATION, sequence.getInterpolation().getName());
		prefs.putBoolean(PREF_REPEAT, sequence.isRepeat());
		prefs.putInt(PREF_FPX, sequence.getFps());
		prefs.putFloat(PREF_SPEED, sequence.getSpeed());

		AudioParameters audio = sequence.getAudioParameters();
		if (audio == null) {
			prefs.remove("audioLow");
			prefs.remove("audioHigh");
			prefs.remove("audioGain");
		} else {
			prefs.putInt("audioLow", audio.getLow());
			prefs.putInt("audioHigh", audio.getHigh());
			prefs.putFloat("audioGain", audio.getGain());
		}

		for (KeyFrame frame : sequence) {
			Preferences framePref = customNode.node(String.valueOf(id++));
			save(frame, framePref);
		}

	}

	@Override
	public boolean isRegions() {
		return false;
	}

	@Override
	public boolean hasOptions() {
		return super.hasOptions() && !isReadOnly();
	}

	@Override
	protected void onSetContext() {
		player = new FramePlayer(getContext().getSchedulerManager().get(Queue.DEVICE_IO),
				getContext().getAudioManager());
	}

	@Override
	protected void onStore(Lit component, CustomOptions controller) throws Exception {
		onSave(getEffectPreferences(component));
	}

	public KeyFrame load(Preferences node) {
		var rows = node.getInt("rows", 0);
		var cols = node.getInt("cols", -1);
		KeyFrameCell[][] frame = null;

		if (rows != 0) {
			frame = new KeyFrameCell[rows][];
			if (cols == -1) {
				/*
				 * Used versions 1.0-SNAPSHOT-24 and earlier, there were only rows, with each
				 * row a long string of colours for each cell
				 */
				for (int i = 0; i < rows; i++) {
					var data = node.get("row" + i, "");
					if (!data.equals("")) {
						var rowRgb = data.split(":");
						var row = new KeyFrameCell[rowRgb.length];
						int col = 0;
						for (String rgb : rowRgb) {
							row[col++] = new KeyFrameCell(new int[] { Integer.parseInt(rgb.substring(0, 2), 16),
									Integer.parseInt(rgb.substring(2, 4), 16),
									Integer.parseInt(rgb.substring(4, 6), 16) }, KeyFrameCellSource.COLOR);
						}
						frame[i] = row;
					}
				}
			} else {
				for (int i = 0; i < rows; i++) {
					var row = new KeyFrameCell[cols];
					for (int j = 0; j < cols; j++) {
						var data = node.get("cell" + i + "," + j, "");
						if (!data.equals("")) {
							var args = data.split(":");
							List<KeyFrameCellSource> l = new ArrayList<>();
							for (String a : args[0].split(",")) {
								l.add(KeyFrameCellSource.valueOf(a));
							}
							var rgb = args[1];
							row[j] = new KeyFrameCell(
									new int[] { Integer.parseInt(rgb.substring(0, 2), 16),
											Integer.parseInt(rgb.substring(2, 4), 16),
											Integer.parseInt(rgb.substring(4, 6), 16) },
									l.toArray(new KeyFrameCellSource[0]));
							row[j].setInterpolation(Interpolation.fromName(args[2]));
						}
					}
					frame[i] = row;
				}
			}
		}
		var keyFrame = new KeyFrame();
		keyFrame.setInterpolation(
				Interpolation.get(node.get(PREF_INTERPOLATION, keyFrame.getInterpolation().getName())));
		keyFrame.setHoldFor(node.getLong(PREF_HOLD_FOR, keyFrame.getHoldFor()));
		keyFrame.setFrame(frame);
		return keyFrame;
	}

	void save(KeyFrame keyFrame, Preferences node) {
		if (readOnly)
			throw new IllegalStateException("This is a read only effect.");
		KeyFrameCell[][] frame = keyFrame.getFrame();
		node.put(PREF_INTERPOLATION, keyFrame.getInterpolation().getName());
		node.putLong(PREF_HOLD_FOR, keyFrame.getHoldFor());
		if (frame != null) {
			int rowCount = 0;
			int colCount = 0;
			for (KeyFrameCell[] row : frame) {
				colCount = 0;

				/* Remove the pre 1.0-SNAPSHOT-24 storage data if there is any */
				node.remove("row" + rowCount);

				for (KeyFrameCell col : row) {
					if (col == null) {
						node.remove("cell" + rowCount + "," + colCount);
					} else {
						StringBuilder b = new StringBuilder();
						b.append(Strings.toSeparatedList(",", Arrays.asList(col.getSources())));
						b.append(":");
						b.append(String.format("%02x%02x%02x", col.getValues()[0], col.getValues()[1],
								col.getValues()[2]));
						b.append(":");
						b.append(col.getInterpolation().getName());
						node.put("cell" + rowCount + "," + colCount, b.toString());
					}
					colCount++;
				}
				rowCount++;
			}
			node.putInt("rows", rowCount);
			node.putInt("cols", colCount);
		}
	}
}
