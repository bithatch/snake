package uk.co.bithatch.snake.ui;

import java.util.Iterator;
import java.util.Stack;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

public class SlideyStack extends StackPane {

	public enum Direction {
		FROM_LEFT, FROM_RIGHT, FROM_TOP, FROM_BOTTOM, FADE_IN, FADE_OUT;

		public Direction opposite() {
			switch (this) {
			case FROM_LEFT:
				return FROM_RIGHT;
			case FROM_RIGHT:
				return FROM_LEFT;
			case FROM_TOP:
				return FROM_BOTTOM;
			case FROM_BOTTOM:
				return FROM_TOP;
			case FADE_IN:
				return FADE_OUT;
			default:
				return FADE_IN;
			}
		}
	}

	class Op {
		Direction dir;
		Node node;

		Op(Direction dir, Node node) {
			this.dir = dir;
			this.node = node;
		}
	}

	private Duration duration = Duration.millis(250);
	private Stack<Op> ops = new Stack<>();

	public Duration getDuration() {
		return duration;
	}

	public void setDuration(Duration duration) {
		this.duration = duration;
	}

	public boolean isEmpty() {
		return ops.isEmpty();
	}

	public void remove(Node node) {
		for (Iterator<Op> opIt = ops.iterator(); opIt.hasNext();) {
			Op op = opIt.next();
			if (op.node == node) {
				opIt.remove();
				getChildren().remove(op.node);
				break;
			}
		}
	}

	public void pop() {
		Op op = ops.pop();
		doAnim(op.dir.opposite(), op.node);
	}

	public void push(Direction dir, Node node) {
		ops.push(new Op(dir, doAnim(dir, node)));
	}

	private Node doAnim(Direction dir, Node node) {
		switch (dir) {
		case FROM_LEFT:
			return slideInFromLeft(node);
		case FROM_RIGHT:
			return slideInFromRight(node);
		case FROM_TOP:
			return slideInFromTop(node);
		case FROM_BOTTOM:
			return slideInFromBottom(node);
		case FADE_IN:
			return fadeIn(node);
		default:
			return fadeOut(node);
		}
	}

	Node fadeIn(Node paneToAdd) {
		var paneToRemove = getChildren().isEmpty() ? null : getChildren().get(0);

		getChildren().add(paneToAdd);
		var fadeInTransition = new FadeTransition(duration);

		if(paneToRemove != null) {
			fadeInTransition.setOnFinished(evt -> {
				getChildren().remove(paneToRemove);
			});
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
			getChildren().remove(paneToRemove);
		});

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

		var keyValue = new KeyValue(paneToAdd.translateXProperty(), 0, Interpolator.EASE_IN);
		var keyFrame = new KeyFrame(duration, keyValue);
		var timeline = new Timeline(keyFrame);
		timeline.setOnFinished(evt -> {
			getChildren().remove(paneToRemove);
		});
		timeline.play();

		return paneToRemove;
	}

	Node slideInFromRight(Node paneToAdd) {
		var paneToRemove = getChildren().get(0);

		paneToAdd.translateXProperty().set(getWidth());
		getChildren().add(paneToAdd);

		var keyValue = new KeyValue(paneToAdd.translateXProperty(), 0, Interpolator.EASE_IN);
		var keyFrame = new KeyFrame(duration, keyValue);
		var timeline = new Timeline(keyFrame);
		timeline.setOnFinished(evt -> {
			getChildren().remove(paneToRemove);
		});
		timeline.play();
		return paneToRemove;
	}

	Node slideInFromBottom(Node paneToAdd) {
		var paneToRemove = getChildren().get(0);

		paneToAdd.translateYProperty().set(getHeight());
		getChildren().add(paneToAdd);

		var keyValue = new KeyValue(paneToAdd.translateYProperty(), 0, Interpolator.EASE_IN);
		var keyFrame = new KeyFrame(duration, keyValue);
		var timeline = new Timeline(keyFrame);
		timeline.setOnFinished(evt -> {
			getChildren().remove(paneToRemove);
		});
		timeline.play();
		return paneToRemove;
	}

	Node slideInFromTop(Node paneToAdd) {
		var paneToRemove = getChildren().get(0);

		paneToAdd.translateYProperty().set(-1 * getHeight());

		getChildren().add(paneToAdd);

		var keyValue = new KeyValue(paneToAdd.translateYProperty(), 0, Interpolator.EASE_IN);
		var keyFrame = new KeyFrame(duration, keyValue);
		var timeline = new Timeline(keyFrame);
		timeline.setOnFinished(evt -> {
			getChildren().remove(paneToRemove);
		});
		timeline.play();
		return paneToRemove;
	}
}
