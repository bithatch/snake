package uk.co.bithatch.snake.ui.effects;

import java.lang.System.Logger.Level;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;

import uk.co.bithatch.jdraw.CompoundBacking;
import uk.co.bithatch.jdraw.DoubleBufferBacking;
import uk.co.bithatch.jdraw.RGBCanvas;
import uk.co.bithatch.jimpulse.Impulse;
import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.lib.effects.Matrix;
import uk.co.bithatch.snake.ui.AudioOptions;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;
import uk.co.bithatch.snake.ui.audio.AudioManager.Listener;
import uk.co.bithatch.snake.ui.drawing.MatrixBacking;

public class AudioEffectHandler extends AbstractPersistentEffectHandler<Matrix, AudioOptions> {

	public final static int[] BLACK = new int[3];

	private AudioEffectMode mode;
	private Matrix effect;
	private int width;
	private int height;
	private Impulse impulse;
	private int[][][] frame;
	private Map<Lit, Listener> listeners = new HashMap<>();
	private CompoundBacking backing;
	private Class<? extends AudioEffectMode> modeClass = ExpandAudioEffectMode.class;
	private int[] color1 = RGBCanvas.RED;
	private int[] color2 = RGBCanvas.ORANGE;
	private int[] color3 = RGBCanvas.GREEN;

	private DoubleBufferBacking buffer;

	private RGBCanvas canvas;

	public AudioEffectHandler() {
		super(Matrix.class, AudioOptions.class);
	}

	@Override
	public boolean isRegions() {
		return false;
	}

	@Override
	public void removed(Device device) {
	}

	@Override
	public void deactivate(Lit component) {
		getContext().getAudioManager().removeListener(listeners.get(component));
	}

	@Override
	public void update(Lit component) {
		Lit.getDevice(component).updateEffect(effect);
	}

	public Matrix getEffect() {
		return effect;
	}

	public CompoundBacking getBacking() {
		return backing;
	}

	@Override
	protected Matrix onActivate(Lit component) {

		
		effect = component.createEffect(Matrix.class);

		/* Load the configuration for this effect */
		onLoad(getEffectPreferences(component), effect);
		
		int[] dw = Lit.getDevice(component).getMatrixSize();
		height = dw[0];
		width = dw[1];

		frame = new int[height][width][3];

		backing = new CompoundBacking(new MatrixBacking(width, height, frame));
		buffer = new DoubleBufferBacking(backing);
		canvas = new RGBCanvas(buffer);
		createMode();
		effect.setCells(frame);
		Listener listener = (snapshot) -> {
			getContext().getSchedulerManager().get(Queue.DEVICE_IO).submit(() -> frame(component, snapshot));
		};
		listeners.put(component, listener);
		getContext().getAudioManager().addListener(listener);
		return effect;
	}

	protected void createMode() {
		try {
			mode = modeClass.getConstructor().newInstance();
		} catch (Exception e) {
			LOG.log(Level.ERROR, "Failed to configured mode. Defaulting.", e);
			mode = new ExpandAudioEffectMode();
		}
		mode.init(canvas, this);
		canvas.reset();
	}

	@Override
	public Class<AudioOptions> getOptionsController() {
		return AudioOptions.class;
	}

	@Override
	public boolean isSupported(Lit component) {
		return true;
	}

	@Override
	public URL getEffectImage(int size) {
		return getContext().getConfiguration().getTheme().getEffectImage(size, "audio");
	}

	@Override
	public String getDisplayName() {
		return bundle.getString("effect.Audio");
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onLoad(Preferences prefs, Matrix effect) {
		try {
			modeClass = (Class<? extends AudioEffectMode>) getClass().getClassLoader()
					.loadClass(prefs.get("mode", PeaksAudioEffectMode.class.getName()));
		} catch (ClassNotFoundException e) {
			modeClass = PeaksAudioEffectMode.class;
		}
		color1 = Colors.fromHex(prefs.get("color1", Colors.toHex(RGBCanvas.RED)));
		color2 = Colors.fromHex(prefs.get("color2", Colors.toHex(RGBCanvas.ORANGE)));
		color3 = Colors.fromHex(prefs.get("color3", Colors.toHex(RGBCanvas.GREEN)));
	}

	@Override
	protected void onSave(Preferences prefs, Matrix effect) {
		prefs.put("mode", mode == null ? "" : modeClass.getName());
		prefs.put("color1", Colors.toHex(color1));
		prefs.put("color2", Colors.toHex(color2));
		prefs.put("color3", Colors.toHex(color3));
	}

	@Override
	protected void onStore(Lit component, AudioOptions controller) throws Exception {
		color1 = controller.getColor1();
		color2 = controller.getColor2();
		color3 = controller.getColor3();
		if (!controller.getMode().equals(modeClass)) {
			modeClass = controller.getMode();
			createMode();
		}
		if (impulse != null) {
			impulse.setSourceIndex(controller.getSource() == null ? 0 : controller.getSource().getIndex());
		}
		mode.update();
		save(component, null);
	}

	void frame(Lit component, double[] audio) {
		try {
			mode.frame(audio);
			buffer.commit();
			update(component);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public Class<? extends Effect> getBackendEffectClass() {
		return Matrix.class;
	}

	public int[] getColor1() {
		return color1;
	}

	public int[] getColor2() {
		return color2;
	}

	public int[] getColor3() {
		return color3;
	}

	public Class<? extends AudioEffectMode> getMode() {
		return modeClass;
	}

}
