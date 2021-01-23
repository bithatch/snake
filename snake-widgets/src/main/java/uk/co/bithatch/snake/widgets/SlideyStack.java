package uk.co.bithatch.snake.widgets;

import java.util.Iterator;
import java.util.Stack;

import javafx.scene.Node; 

public class SlideyStack extends AnimPane {

	class Op {
		Direction dir;
		Node node;

		Op(Direction dir, Node node) {
			this.dir = dir;
			this.node = node;
		}
	}

	private Stack<Op> ops = new Stack<>();

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

}
