package uk.co.bithatch.snake.ui.audio;

import java.io.Closeable;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.animation.AudioDataProvider;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.Configuration;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;

public class AudioManager implements Closeable, PreferenceChangeListener, AudioDataProvider {

	final static System.Logger LOG = System.getLogger(AudioManager.class.getName());

	public interface Listener {
		void snapshot(double[] snapshot);
	}

	public interface VolumeListener {
		void volumeChanged(Device sink, int volume);
	}

	public static class AudioSource {
		private int index;
		private String name;

		public AudioSource(int index, String name) {
			super();
			this.index = index;
			this.name = name;
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}
	}

	public static class AudioSink {
		private int index;
		private String name;

		public AudioSink(int index, String name) {
			super();
			this.index = index;
			this.name = name;
		}

		public int getIndex() {
			return index;
		}

		public void setIndex(int index) {
			this.index = index;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String toString() {
			return name;
		}
	}

	private App context;
	private double[] snapshot = new double[256];
	private List<Listener> listeners = Collections.synchronizedList(new ArrayList<>());
	private List<VolumeListener> volumeListeners = Collections.synchronizedList(new ArrayList<>());
	private AudioBackend backend;
	private ScheduledFuture<?> grabTask;
	private Configuration cfg;
	private ScheduledExecutorService queue;
	private Throwable error;

	public AudioManager(App context) {
		this.context = context;
		cfg = context.getConfiguration();
		queue = context.getSchedulerManager().get(Queue.AUDIO);
		cfg.getNode().addPreferenceChangeListener(this);
		backend = createBackend();
	}

	public boolean hasVolume(Device device) {
		for (AudioSink sink : getSinks()) {
			if (sink.getName().startsWith(device.getName())) {
				return true;
			}
		}
		return false;
	}

	public boolean isMuted(Device device) {
		for (AudioSink sink : getSinks()) {
			if (sink.getName().startsWith(device.getName())) {
				return backend.isMuted(sink);
			}
		}
		return false;
	}

	public void setMuted(Device device, boolean muted) {
		for (AudioSink sink : getSinks()) {
			if (sink.getName().startsWith(device.getName())) {
				backend.setMuted(sink, muted);
				return;
			}
		}
	}

	public int getVolume(Device device) {
		for (AudioSink sink : getSinks()) {
			if (sink.getName().startsWith(device.getName())) {
				return backend.getVolume(sink);
			}
		}
		return 0;
	}

	public void setVolume(Device device, int volume) {
		for (AudioSink sink : getSinks()) {
			if (sink.getName().startsWith(device.getName())) {
				System.out.println();
				backend.setVolume(sink, volume);
				return;
			}
		}
	}

	public List<AudioSource> getSources() {
		return backend == null ? Collections.emptyList() : backend.getSources();
	}

	public List<AudioSink> getSinks() {
		return backend == null ? Collections.emptyList() : backend.getSinks();
	}

	public Throwable getError() {
		return error;
	}

	public void removeListener(Listener listener) {
		listeners.remove(listener);

	}

	public void addListener(Listener listener) {
		synchronized (listeners) {
			checkMonitoring();
			listeners.add(listener);
		}
	}

	public void removeVolumeListener(VolumeListener listener) {
		volumeListeners.remove(listener);

	}

	public void addVolumeListener(VolumeListener listener) {
		synchronized (listeners) {
			volumeListeners.add(listener);
		}
	}

	protected void checkMonitoring() {
		if (grabTask == null) {
			try {
				AudioSource source = getSource();
				if (source != null)
					backend.setSourceIndex(source.getIndex());
				ScheduledExecutorService queue = context.getSchedulerManager().get(Queue.AUDIO);
				queue.submit(() -> backend.init());
				scheduleGrabbing(queue);
			} catch (Exception e) {
				LOG.log(Level.ERROR, "Failed to initialise audio system backend. No audio features will be available.");
				error = e;
			}
		}
	}

	public void setSource(AudioSource source) {
		context.getConfiguration().setAudioSource(source.getName());
	}

	public AudioSource getSource() {
		AudioSource source = getSource(context.getConfiguration().getAudioSource());
		if (source == null) {
			List<AudioSource> sources = getSources();
			return sources.isEmpty() ? null : sources.get(0);
		}
		return source;
	}

	public AudioSource getSource(int index) {
		for (AudioSource s : getSources()) {
			if (s.getIndex() == index)
				return s;
		}
		return null;
	}

	public AudioSource getSource(String name) {
		for (AudioSource s : getSources()) {
			if (name.equals(s.getName()))
				return s;
		}
		return null;
	}

	protected void scheduleGrabbing(ScheduledExecutorService queue) {
		int delay = 1000 / Math.max(1, cfg.getAudioFPS());
		grabTask = queue.scheduleAtFixedRate(() -> grab(), 0, delay, TimeUnit.MILLISECONDS);
	}

	public double[] getSnapshot() {
		synchronized (listeners) {
			checkMonitoring();
			return snapshot;
		}
	}

	@Override
	public void close() throws IOException {
		if (grabTask != null)
			grabTask.cancel(false);
		if (backend != null) {
			backend.stop();
			backend = null;
		}
	}

	void grab() {
		synchronized (listeners) {
			snapshot = backend.getSnapshot(cfg.isAudioFFT());
			float gain = cfg.getAudioGain();
			if (gain != 1) {
				for (int i = 0; i < snapshot.length; i++)
					snapshot[i] = Math.min(1, snapshot[i] * gain);
			}
			for (int i = listeners.size() - 1; i >= 0; i--)
				listeners.get(i).snapshot(snapshot);
		}
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent evt) {
		if (evt.getKey().equals(Configuration.PREF_AUDIO_SOURCE)) {
			if (backend != null) {
				queue.submit(() -> backend.setSourceIndex(getSource().getIndex()));
			}
		} else if (evt.getKey().equals(Configuration.PREF_AUDIO_FPS)) {
			if (grabTask != null)
				grabTask.cancel(false);
			scheduleGrabbing(queue);
		}
	}

	protected AudioBackend createBackend() {
		try {
			return new JImpulseAudioBackend(this);
		} catch (UnsatisfiedLinkError | Exception e) {
			error = e;
			if (LOG.isLoggable(Level.DEBUG))
				LOG.log(Level.ERROR, "Failed to setup audio backend. Do you have libfftw-3 installed?", e);
			else
				LOG.log(Level.ERROR,
						"Failed to setup audio backend. Do you have libfftw-3 installed? " + e.getLocalizedMessage());
			return new AudioBackend() {

				@Override
				public void setSourceIndex(int index) {
				}

				@Override
				public void init() {
				}

				@Override
				public void stop() {
				}

				@Override
				public double[] getSnapshot(boolean audioFFT) {
					return null;
				}

				@Override
				public List<AudioSource> getSources() {
					return Collections.emptyList();
				}

				@Override
				public List<AudioSink> getSinks() {
					return Collections.emptyList();
				}

				@Override
				public int getVolume(AudioSink sink) {
					return 0;
				}

				@Override
				public void setVolume(AudioSink sink, int volume) {
				}

				@Override
				public boolean isMuted(AudioSink sink) {
					return false;
				}

				@Override
				public void setMuted(AudioSink sink, boolean muted) {
				}

			};
		}
	}

	@SuppressWarnings("resource")
	void fireVolumeChange(AudioSink sink) {
		int vol = backend.getVolume(sink);
		try {
			for (Device device : context.getBackend().getDevices()) {
				if (sink.getName().startsWith(device.getName())) {
					for (int i = 0; i < volumeListeners.size(); i++)
						volumeListeners.get(i).volumeChanged(device, vol);
					return;
				}
			}
		} catch (Exception e) {
			LOG.log(Level.ERROR, "Failed to get devices.");
		}

	}
}
