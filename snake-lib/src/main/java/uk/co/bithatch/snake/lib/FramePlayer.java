package uk.co.bithatch.snake.lib;

import java.util.ArrayList;
import java.util.Collections;
//import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
//import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeUnit;

import uk.co.bithatch.snake.lib.effects.Matrix;

public class FramePlayer {

	public interface FrameListener {
		void frameUpdate(KeyFrame frame, int[][][] rgb, float fac, long frameNumber);

		void pause(boolean pause);

		void started(Sequence sequence, Device device);

		void stopped();
	}

	private Device device;
	private Matrix effect;
	private ScheduledExecutorService executor;
	private int index;
	private int lastDrawnIndex;
	private List<FrameListener> listeners = Collections.synchronizedList(new ArrayList<>());
	private boolean paused;
	private long pauseStarted;
	private boolean run;
	private Sequence sequence;
	private long started;
	private ScheduledFuture<?> task;
	private Runnable waitingTask;
	private long waitingTime;
	private TimeUnit waitingUnit;
	private ScheduledFuture<?> elapsedTimer;
	private long targetNewTimeElapsed;
	private long lastTimeElapsed;

	public FramePlayer(ScheduledExecutorService executor) {
		this.executor = executor;
	}

	public void addListener(FrameListener listener) {
		listeners.add(listener);
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public Matrix getEffect() {
		return effect;
	}

	public void setEffect(Matrix effect) {
		this.effect = effect;
	}

	public Sequence getSequence() {
		return sequence;
	}

	public void setSequence(Sequence sequence) {
		this.sequence = sequence;
	}

	public KeyFrame getFrame() {
		long now = getTimeElapsed();
		KeyFrame playFrame = null;
		for (int i = 0; i < sequence.size(); i++) {
			KeyFrame kf = sequence.get(i);
			if (now >= kf.getIndex()) {
				playFrame = kf;
			} else if (playFrame != null)
				return playFrame;
		}
		return sequence.get(sequence.size() - 1);
	}

	public long getFrameNumber() {
		return started == 0 ? 0 : (long) ((double) sequence.getFps() * ((double) getTimeElapsed() / 1000.0));
	}

	public long getTimeElapsed() {
		if (paused) {
			return pauseStarted - started;
		} else {
			return started == 0 ? 0 : (long) ((double) (System.currentTimeMillis() - started) * (sequence.getSpeed()));
		}
	}

	public boolean isPaused() {
		return paused;
	}

	public boolean isActive() {
		return isPlaying() && !isPaused();
	}

	public boolean isPlaying() {
		return run;
	}

	public void play() {
		if (run)
			throw new IllegalStateException("Already playing.");
		if (device == null)
			throw new IllegalStateException("No device set.");
		if (sequence == null)
			throw new IllegalStateException("No sequence set.");
		if (effect == null)
			throw new IllegalStateException("No effect set.");
		this.index = 0;
		this.lastDrawnIndex = -1;
		if (sequence.isEmpty())
			return;
		this.run = true;
		started = System.currentTimeMillis();
		executor.execute(() -> {
			fireStarted(sequence, device);
			playFrame(effect);
		});
	}

	public void removeListener(FrameListener listener) {
		listeners.remove(listener);
	}

	public void restart() {
		if (device == null || sequence == null)
			throw new IllegalStateException("Never started.");
		if (isActive()) {
			executor.execute(() -> {
				if(isPlaying())
					stop();
				executor.execute(() -> play());
			});
		} else {
			executor.execute(() -> play());
		}
	}

	public void setPaused(boolean paused) {
		if (paused != this.paused) {
			this.paused = paused;
			if (paused) {
				pauseStarted = System.currentTimeMillis();
			} else if (!paused) {
				started += System.currentTimeMillis() - pauseStarted;
				if (waitingTask != null) {
					schedule(waitingTask, waitingTime, waitingUnit);
					clearWaitingTask();
				}
			}
			executor.execute(() -> firePaused(paused));
		}
	}

	protected void clearWaitingTask() {
		waitingTask = null;
		waitingTime = 0;
		waitingUnit = null;
	}

	public void setTimeElapsed(long newTimeElapsed) {
		if (isPlaying()) {
			targetNewTimeElapsed = newTimeElapsed;
			/*
			 * This limits the rate at which the time elapsed changes to the current FPS. If
			 * this is not done, the LED's may lag.
			 */
			if (elapsedTimer == null) {
				lastTimeElapsed = -1;
				elapsedTimer = executor.scheduleAtFixedRate(() -> {
					if (lastTimeElapsed == targetNewTimeElapsed) {
						elapsedTimer.cancel(false);
						elapsedTimer = null;
					} else {
						lastTimeElapsed = targetNewTimeElapsed;
						doSetTimeElapsed(targetNewTimeElapsed);
					}
				}, getFrameTime(), getFrameTime(), TimeUnit.MILLISECONDS);
			}
		} else
			throw new IllegalStateException("Must be paused to be able to set time elapsed.");

	}

	protected void doSetTimeElapsed(long newTimeElapsed) {
		/*
		 * Get the difference in time between the frame we are currently on and the time
		 * we wish to seek to. Then adjust the start time (and pause time) accordingly
		 */
		long timeElapsed = getTimeElapsed();
		long diff = newTimeElapsed - timeElapsed;

		started -= diff;
		KeyFrame frame = getFrame();
		index = sequence.indexOf(frame);
		Interpolation ip = getFrameInterpolation(frame);
		float progress = ip.equals(Interpolation.none) ? 0 : getFrameProgress(newTimeElapsed - frame.getIndex(), frame);
		KeyFrame nextFrame = getNextFrame(frame);
		int[][][] rgb = getRGB(frame, nextFrame, progress);
		drawFrame(effect, rgb);
		fireFrameUpdate(frame, rgb, progress);
	}

	public void stop() {
		if (!run)
			throw new IllegalStateException("Not running.");

		if (paused) {
			started += System.currentTimeMillis() - pauseStarted;
			paused = false;
			clearWaitingTask();
		}
		run = false;
		if (task != null) {
			task.cancel(false);
			task = null;
		}
		executor.execute(() -> ended());
	}

	protected int[][][] drawFrame(Matrix effect, int[][][] rgbs) {
		effect.setCells(rgbs);
		device.updateEffect(effect);
		return rgbs;
	}

	protected void ended() {
		started = 0;
		KeyFrame ff = sequence.get(0);
		int[][][] rgb = ff.getFrame();
		drawFrame(effect, rgb);
		fireFrameUpdate(ff, rgb, 0);
		fireStopped();
	}

	void fireFrameUpdate(KeyFrame keyFrame, int[][][] rgb, float fac) {
		synchronized (listeners) {
			long f = getFrameNumber();
			for (int i = listeners.size() - 1; i >= 0; i--) {
				listeners.get(i).frameUpdate(keyFrame, rgb, fac, f);
			}
		}
	}

	void firePaused(boolean pause) {
		synchronized (listeners) {
			for (int i = listeners.size() - 1; i >= 0; i--) {
				listeners.get(i).pause(pause);
			}
		}
	}

	void fireStarted(Sequence seq, Device device) {
		synchronized (listeners) {
			for (int i = listeners.size() - 1; i >= 0; i--) {
				listeners.get(i).started(seq, device);
			}
		}
	}

	void fireStopped() {
		synchronized (listeners) {
			for (int i = listeners.size() - 1; i >= 0; i--) {
				listeners.get(i).stopped();
			}
		}
	}

	Interpolation getFrameInterpolation(KeyFrame playFrame) {
		return playFrame.getInterpolation() == null || playFrame.getInterpolation().equals(Interpolation.sequence)
				? sequence.getInterpolation()
				: playFrame.getInterpolation();
	}

	float getFrameProgress(long now, KeyFrame playFrame) {
		/** Work out how far along the frame we are */
		return (float) ((double) now / (double) playFrame.getActualLength());
	}

	KeyFrame getNextFrame(KeyFrame frame) {
		int idx = sequence.indexOf(frame);
		KeyFrame nextFrame = idx < sequence.size() - 1 ? sequence.get(idx + 1) : null;
		if (nextFrame == null) {
			nextFrame = sequence.get(0);
		}
		return nextFrame;
	}

	int[][][] getRGB(KeyFrame frame, KeyFrame nextFrame, float frac) {
		return frame.getInterpolatedFrame(nextFrame, frac);
	}

	void playFrame(Matrix effect) {
		try {
			/*
			 * Work out which keyframe we should be on. Most of the time, this will be the
			 * first hit. It take more iterations if the timeline position is changed while
			 * running
			 */
			long now = (long) ((double) (System.currentTimeMillis() - started) * sequence.getSpeed());
			KeyFrame playFrame = null;
			int playIndex = -1;
			for (int i = index; i < sequence.size(); i++) {
				KeyFrame kf = sequence.get(i);
				if (now >= kf.getIndex()) {
					/*
					 * It is at least this frame, check the next and exit it if 'now' is not yet at
					 * the keyframes index
					 */
					playFrame = kf;
					playIndex = i;
				} else if (playFrame != null)
					break;
			}

			if (playFrame == null) {
				/* This should be impossible */
				restart();
				return;
			}
			index = playIndex;
			float frac = getFrameProgress(now - playFrame.getIndex(), playFrame);

			/* Decide on interpolation */
			Interpolation interpolation = getFrameInterpolation(playFrame);

			/*
			 * If no interpolation, draw the first frame only, but keep running at the same
			 * refresh rate until the next key frame starts
			 */
			if (interpolation.equals(Interpolation.none)) {
				int[][][] rgbs = playFrame.getFrame();
				if (index != lastDrawnIndex) {
					lastDrawnIndex = index;

					drawFrame(effect, rgbs);
				}
				fireFrameUpdate(playFrame, rgbs, 0);
				if (frac >= 1) {
					if (sequence.isRepeat()) {
						/* Repeating, so reset all the indexes and start point */
						started = System.currentTimeMillis();
						index = 0;
					} else {
						run = false;
					}
					frac = 1;
				}
			} else {

				/*
				 * Get the next frame. This will be the next in the sequence if there is one,
				 * the first frame if this frame is the last and repeat mode is on, or null if
				 * this would be the first frame and repeat mode is off
				 */
				KeyFrame nextFrame = playIndex < sequence.size() - 1 ? sequence.get(playIndex + 1) : null;
				if (nextFrame == null) {
					if (sequence.isRepeat()) {
						/* At the end ? */
						if (frac >= 1) {
							/* Repeating, so reset all the indexes and start point */
							started = System.currentTimeMillis();
							index = 0;
							frac = 1;
						}
					} else {
						/* At the end ? */
						if (frac >= 1) {
							run = false;
							frac = 1;
						}
					}
					nextFrame = sequence.get(0);
				}

				int[][][] rgbs = playFrame.getInterpolatedFrame(nextFrame, interpolation.apply(frac));
				drawFrame(effect, rgbs);
				fireFrameUpdate(playFrame, rgbs, frac);
			}

			/* Schedule the next frame */
			if (run)
				schedule(() -> playFrame(effect), getFrameTime(), TimeUnit.MILLISECONDS);
			else {
				ended();
			}

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	protected int getFrameTime() {
		return 1000 / sequence.getFps();
	}

	void schedule(Runnable r, long t, TimeUnit u) {
		if (paused) {
			if (waitingTask == null) {
				waitingTask = r;
				waitingTime = t;
				waitingUnit = u;
				return;
			} else
				throw new IllegalStateException("Cannot submit any tasks when paused.");
		}
		if(!executor.isShutdown())
			task = executor.schedule(r, t, u);
	}

	public boolean isReady() {
		return device != null && sequence != null && effect != null;
	}
}
