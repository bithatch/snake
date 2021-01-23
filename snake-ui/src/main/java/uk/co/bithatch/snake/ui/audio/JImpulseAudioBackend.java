package uk.co.bithatch.snake.ui.audio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import uk.co.bithatch.jimpulse.Impulse;
import uk.co.bithatch.snake.ui.audio.AudioManager.AudioSink;
import uk.co.bithatch.snake.ui.audio.AudioManager.AudioSource;

/**
 * Deprecated. This will be replaced with proper Java pulse bindings,
 */
@Deprecated
public class JImpulseAudioBackend implements AudioBackend {

	private Impulse impulse;
	private AudioManager manager;

	public JImpulseAudioBackend(AudioManager manager) {
		impulse = new Impulse();
		this.manager = manager;
	}

	@Override
	public void setSourceIndex(int index) {
		impulse.setSourceIndex(index);
	}

	@Override
	public void init() {
		impulse.initImpulse();
	}

	@Override
	public void stop() {
		impulse.stop();
	}

	@Override
	public double[] getSnapshot(boolean fft) {
		return impulse.getSnapshot(fft);
	}

	@Override
	public List<AudioSource> getSources() {

		/* TODO add this to jimpulse so we can get a list of devices more generally? */
		/* TODO or make more efficient and not load so much */
		ProcessBuilder pb = new ProcessBuilder("pacmd", "list-sources");
		pb.redirectErrorStream(true);
		try {
			Process p = pb.start();
			List<AudioSource> l = new ArrayList<>();
			String name = null;
			int index = -1;
			try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = r.readLine()) != null) {
					line = line.trim();
					if(line.startsWith("*")) {
						/* Don't care if active or not */
						line = line.substring(2);
					}
					if (line.startsWith("index: ")) {
						if (name != null && index != -1) {
							l.add(new AudioSource(index, name));
							name = null;
						}
						index = Integer.parseInt(line.substring(7));
					}
					if (index != -1 && line.startsWith("name: ")) {
						name = line.substring(6);
					}
					if (index != -1 && line.startsWith("device.description = \"")) {
						name = line.substring(22, line.length() - 1);
					}
				}
			}
			if (name != null && index != -1) {
				l.add(new AudioSource(index, name));
			}
			try {
				int ret = p.waitFor();
				if (ret != 0)
					throw new IOException(String.format("Could not list sources. Exited with value %d.", ret));
			} catch (InterruptedException ie) {
			}
			return l;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to get sources.");
		}
	}

	@Override
	public List<AudioSink> getSinks() {

		/* TODO add this to jimpulse so we can get a list of devices more generally? */
		/* TODO or make more efficient and not load so much */
		ProcessBuilder pb = new ProcessBuilder("pacmd", "list-sinks");
		pb.redirectErrorStream(true);
		try {
			Process p = pb.start();
			List<AudioSink> l = new ArrayList<>();
			String name = null;
			int index = -1;
			try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = r.readLine()) != null) {
					line = line.trim();
					if(line.startsWith("*")) {
						/* Don't care if active or not */
						line = line.substring(2);
					}
					if (line.startsWith("index: ")) {
						if (name != null && index != -1) {
							l.add(new AudioSink(index, name));
							name = null;
						}
						index = Integer.parseInt(line.substring(7));
					}
					if (index != -1 && line.startsWith("name: ")) {
						name = line.substring(6);
					}
					if (index != -1 && line.startsWith("device.description = \"")) {
						name = line.substring(22, line.length() - 1);
					}
				}
			}
			if (name != null && index != -1) {
				l.add(new AudioSink(index, name));
			}
			try {
				int ret = p.waitFor();
				if (ret != 0)
					throw new IOException(String.format("Could not list sources. Exited with value %d.", ret));
			} catch (InterruptedException ie) {
			}
			return l;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to get sources.");
		}
	}

	@Override
	public int getVolume(AudioSink sink) {
		ProcessBuilder pb = new ProcessBuilder("pacmd", "list-sinks");
		pb.redirectErrorStream(true);
		try {
			Process p = pb.start();
			boolean read = false;
			try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = r.readLine()) != null) {
					line = line.trim();
					if (line.startsWith("index: ")) {
						if (read)
							break;
						int index = Integer.parseInt(line.substring(7));
						if (index == sink.getIndex()) {
							read = true;
						}
					}
					if (read) {
						if (line.startsWith("volume: ")) {
							String[] parts = line.split("\\s+");
							int ch = 0;
							int t = 0;
							for (String pa : parts) {
								if (pa.endsWith("%")) {
									ch++;
									t += Integer.parseInt(pa.substring(0, pa.length() - 1));
								}
							}
							System.out.println("tv " + t + " of " + ch + " for " + sink.getName() + " : "
									+ sink.getIndex() + " " + line);
							return t / ch;
						}
					}
				}
			} finally {
				try {
					int ret = p.waitFor();
					if (ret != 0)
						throw new IOException(String.format("Could not get volume. Exited with value %d.", ret));
				} catch (InterruptedException ie) {
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to get volume.");
		}
		return 0;
	}

	@Override
	public void setVolume(AudioSink sink, int volume) {
		int volVal = (int) ((float) 65536 * ((float) volume / 100.0));
		System.out.println("set vol " + sink.getIndex() + " to " + volVal);
		ProcessBuilder pb = new ProcessBuilder("pacmd", "set-sink-volume", String.valueOf(sink.getName()),
				String.valueOf(volVal));
		pb.redirectErrorStream(true);
		try {
			Process p = pb.start();
			try {
				p.getInputStream().transferTo(System.out);
				int ret = p.waitFor();
				if (ret != 0)
					throw new IOException(String.format("Could not set volume. Exited with value %d.", ret));
				manager.fireVolumeChange(sink);
			} catch (InterruptedException ie) {
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to set volume.");
		}

	}

	@Override
	public boolean isMuted(AudioSink sink) {
		ProcessBuilder pb = new ProcessBuilder("pacmd", "list-sinks");
		pb.redirectErrorStream(true);
		try {
			Process p = pb.start();
			boolean read = false;
			try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = r.readLine()) != null) {
					line = line.trim();
					if (line.startsWith("index: ")) {
						if (read)
							break;
						int index = Integer.parseInt(line.substring(7));
						if (index == sink.getIndex()) {
							read = true;
						}
					}
					if (read) {
						if (line.startsWith("muted: ")) {
							return line.substring(7).equals("yes");
						}
					}
				}
			} finally {
				try {
					int ret = p.waitFor();
					if (ret != 0)
						throw new IOException(String.format("Could not list sources. Exited with value %d.", ret));
				} catch (InterruptedException ie) {
				}
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to get sources.");
		}
		return false;
	}

	@Override
	public void setMuted(AudioSink sink, boolean muted) {
		ProcessBuilder pb = new ProcessBuilder("pacmd", "set-sink-mute", String.valueOf(sink.getIndex()),
				String.valueOf(muted));
		pb.redirectErrorStream(true);
		try {
			Process p = pb.start();
			try {
				p.getInputStream().transferTo(System.out);
				int ret = p.waitFor();
				if (ret != 0)
					throw new IOException(String.format("Could not mute. Exited with value %d.", ret));
				manager.fireVolumeChange(sink);
			} catch (InterruptedException ie) {
			}
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Failed to set volume.");
		}

	}

}
