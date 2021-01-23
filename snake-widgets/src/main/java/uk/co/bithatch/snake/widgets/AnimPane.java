package uk.co.bithatch.snake.widgets;

import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.layout.StackPane; 
import javafx.util.Duration;

public class AnimPane extends StackPane {

	private Duration duration = Duration.millis(500);
	private Interpolator interpolator = Interpolator.EASE_BOTH;
	private Animation waiting;

	public Interpolator getInterpolator() {
		return interpolator;
	}

	public void setInterpolator(Interpolator interpolator) {
		this.interpolator = interpolator;
	}

	public Duration getDuration() {
		return duration;
	}

	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	public void setContent(Direction dir, Node node) {
		doAnim(dir, node);
	}

	protected Node doAnim(Direction dir, Node node) {
		if(waiting != null) {
			waiting.stop();
			waiting.getOnFinished().handle(null);
			waiting = null;
		}
		switch (dir) {
		case FROM_LEFT:
			return slideInFromLeft(node);
		case FROM_RIGHT:
			return slideInFromRight(node);
		case FROM_TOP:
			return slideInFromTop(node);
		case FROM_BOTTOM:
			return slideInFromBottom(node);
		default:
			return fadeIn(node);
		}
	}

	Node fadeIn(Node paneToAdd) {
		var paneToRemove = getChildren().isEmpty() ? null : getChildren().get(0);

		getChildren().add(paneToAdd);
		var fadeInTransition = new FadeTransition(duration);
		
		if (paneToRemove != null) {
			var fadeOutTransition = new FadeTransition(duration);
			fadeOutTransition.setOnFinished(evt -> {
				waiting = null;
				getChildren().remove(paneToRemove);
			});
			fadeOutTransition.setNode(paneToRemove);
			fadeOutTransition.setFromValue(1);
			fadeOutTransition.setToValue(0);
			fadeOutTransition.play();
			waiting = fadeOutTransition;
		}
		fadeInTransition.setNode(paneToAdd);
		fadeInTransition.setFromValue(0);
		fadeInTransition.setToValue(1);
		fadeInTransition.play();

		return paneToRemove;
	}

	Node fadeOut(Node paneToAdd) {
		var paneToRemove = getChildren().get(0);
		getChildren().add(0, paneToAdd);

		var fadeOutTransition = new FadeTransition(duration);

		fadeOutTransition.setOnFinished(evt -> {
			waiting = null;
			getChildren().remove(paneToRemove);
		});
		waiting = fadeOutTransition;

		fadeOutTransition.setNode(paneToRemove);
		fadeOutTransition.setFromValue(1);
		fadeOutTransition.setToValue(0);
		fadeOutTransition.play();

		return paneToRemove;
	}

	Node slideInFromLeft(Node paneToAdd) {
		var paneToRemove = getChildren().get(0);

		paneToAdd.translateXProperty().set(-1 * getWidth());
		getChildren().add(paneToAdd);

		var keyValue = new KeyValue(paneToAdd.translateXProperty(), 0, interpolator);
		KeyValue outKeyValue = new KeyValue(paneToRemove.translateXProperty(), getWidth(), interpolator);
		var keyFrame = new KeyFrame(duration, keyValue, outKeyValue);
		var timeline = new Timeline(keyFrame);
		timeline.setOnFinished(evt -> {
			getChildren().remove(paneToRemove);
		});
		timeline.play();
		waiting = timeline;

		return paneToRemove;
	}

	Node slideInFromRight(Node paneToAdd) {
		var paneToRemove = getChildren().get(0);

		paneToAdd.translateXProperty().set(getWidth());
		getChildren().add(paneToAdd);

		var keyValue = new KeyValue(paneToAdd.translateXProperty(), 0, interpolator);
		KeyValue outKeyValue = new KeyValue(paneToRemove.translateXProperty(), -1 * getWidth(), interpolator);
		var keyFrame = new KeyFrame(duration, keyValue, outKeyValue);
		var timeline = new Timeline(keyFrame);
		timeline.setOnFinished(evt -> {
			waiting = null;
			getChildren().remove(paneToRemove);
		});
		timeline.play();
		waiting = timeline;
		return paneToRemove;
	}

	Node slideInFromBottom(Node paneToAdd) {
		var paneToRemove = getChildren().get(0);

		paneToAdd.translateYProperty().set(getHeight());
		getChildren().add(paneToAdd);

		var keyValue = new KeyValue(paneToAdd.translateYProperty(), 0, interpolator);
		KeyValue outKeyValue = new KeyValue(paneToRemove.translateYProperty(), -getHeight(), interpolator);
		var keyFrame = new KeyFrame(duration, keyValue, outKeyValue);
		var timeline = new Timeline(keyFrame);
		timeline.setOnFinished(evt -> {
			waiting = null;
			getChildren().remove(paneToRemove);
		});
		timeline.play();
		waiting = timeline;
		return paneToRemove;
	}

	Node slideInFromTop(Node paneToAdd) {
		var paneToRemove = getChildren().get(0);

		paneToAdd.translateYProperty().set(-1 * getHeight());

		getChildren().add(paneToAdd);

		var keyValue = new KeyValue(paneToAdd.translateYProperty(), 0, interpolator);
		KeyValue outKeyValue = new KeyValue(paneToRemove.translateYProperty(), getHeight(), interpolator);
		var keyFrame = new KeyFrame(duration, keyValue, outKeyValue);
		var timeline = new Timeline(keyFrame);
		timeline.setOnFinished(evt -> {
			waiting = null;
			getChildren().remove(paneToRemove);
		});
		timeline.play();
		waiting = timeline;
		return paneToRemove;
	}

	public Node getContent() {
		return getChildren().isEmpty() ? null : getChildren().get(getChildren().size() - 1);
	}
}
