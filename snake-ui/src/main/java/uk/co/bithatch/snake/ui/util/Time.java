package uk.co.bithatch.snake.ui.util;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.util.Duration;

public class Time {

	public static class Timer {

		private Timeline timer;
		private Duration duration;
		private EventHandler<ActionEvent> task;

		public Timer(Duration duration, EventHandler<ActionEvent> task) {
			this.duration = duration;
			this.task = task;
		}

		public synchronized void reset() {
			if (timer == null) {
				timer = new Timeline(new KeyFrame(duration, task));
			}
			timer.playFromStart();
		}

		public synchronized void cancel() {
			if (timer != null) {
				timer.stop();
				timer = null;
			}
		}
	}

	public static String formatTime(long millis) {
		java.time.Duration diff = java.time.Duration.ofMillis(millis);
		return String.format("%d:%02d:%02d", diff.toHours(), diff.toMinutesPart(), diff.toSecondsPart());
	}
}
