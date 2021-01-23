package uk.co.bithatch.snake.ui.audio;

import java.util.List;

import uk.co.bithatch.snake.ui.audio.AudioManager.AudioSink;
import uk.co.bithatch.snake.ui.audio.AudioManager.AudioSource;

public interface AudioBackend {

	public interface Listener {
		void volumeChanged(int volume);
	}

	void setSourceIndex(int index);

	void init();

	void stop();

	double[] getSnapshot(boolean audioFFT);

	List<AudioSource> getSources();

	List<AudioSink> getSinks();

	int getVolume(AudioSink sink);

	void setVolume(AudioSink sink, int volume);

	boolean isMuted(AudioSink sink);

	void setMuted(AudioSink sink, boolean muted);

}
