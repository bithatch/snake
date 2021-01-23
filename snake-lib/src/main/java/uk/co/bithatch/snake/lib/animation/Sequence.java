package uk.co.bithatch.snake.lib.animation;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class Sequence extends AbstractList<KeyFrame> {
	private List<KeyFrame> backing = new ArrayList<>();
	private int fps = 10;
	private Interpolation interpolation = Interpolation.linear;
	private boolean repeat = true;
	private float speed = 1.f;
	private long totalFrames;
	private long totalLength;
	private AudioParameters audioParameters;

	public Sequence() {
	}

	public Sequence(Sequence sequence) {
		this.fps = sequence.fps;
		this.interpolation = sequence.interpolation;
		this.repeat = sequence.repeat;
		this.speed =  sequence.speed;
		this.totalFrames = sequence.totalFrames;
		this.totalLength = sequence.totalLength;
		for(KeyFrame f : sequence.backing) {
			this.backing.add(new KeyFrame(f));
		}
	}

	public AudioParameters getAudioParameters() {
		return audioParameters;
	}

	public void setAudioParameters(AudioParameters audioParameters) {
		this.audioParameters = audioParameters;
	}

	@Override
	public void add(int index, KeyFrame e) {
		try {
			e.setSequence(this);
			backing.add(index, e);
		} finally {
			rebuildTimestats();
		}
	}

	@Override
	public boolean add(KeyFrame e) {
		try {
			e.setSequence(this);
			return backing.add(e);
		} finally {
			rebuildTimestats();
		}
	}

	@Override
	public void clear() {
		backing.clear();
		rebuildTimestats();
	}

	@Override
	public KeyFrame get(int index) {
		return backing.get(index);
	}

	public int getFps() {
		return fps;
	}

	public Interpolation getInterpolation() {
		return interpolation;
	}

	public float getSpeed() {
		return speed;
	}

	public long getTotalFrames() {
		return totalFrames;
	}

	public long getTotalLength() {
		return totalLength;
	}

	public boolean isRepeat() {
		return repeat;
	}

	@Override
	public KeyFrame remove(int index) {
		try {
			return backing.remove(index);
		} finally {
			rebuildTimestats();
		}
	}

	public void setFps(int fps) {
		this.fps = fps;
		rebuildTimestats();
	}

	public void setInterpolation(Interpolation interpolation) {
		this.interpolation = interpolation;
	}

	public void setRepeat(boolean repeat) {
		this.repeat = repeat;
	}

	public void setSpeed(float speed) {
		this.speed = speed;
		rebuildTimestats();
	}

	@Override
	public int size() {
		return backing.size();
	}

	void rebuildTimestats() {
		totalFrames = 0;
		totalLength = 0;
		for (KeyFrame f : this) {
			f.setIndex(totalLength);
			f.setStartFrame(totalFrames);
			long keyFrameLength = f.getActualLength();
			long keyFrameFrames = f.getActualFrames();
			totalFrames += keyFrameFrames;
			totalLength += keyFrameLength;
		}
	}

	public KeyFrame getFrameAt(long l) {
		KeyFrame playFrame = null;
		for (int i = 0; i < size(); i++) {
			KeyFrame kf = get(i);
			if (l >= kf.getIndex()) {
				playFrame = kf;
			} else if (playFrame != null)
				return playFrame;
		}
		return get(size() - 1);
	}

}
