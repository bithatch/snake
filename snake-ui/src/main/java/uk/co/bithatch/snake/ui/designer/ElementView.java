package uk.co.bithatch.snake.ui.designer;

import javafx.scene.Node;
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.IO;

public class ElementView {
	private ComponentType type;
	private Tool elementTool;
	private Node label;
	private IO element;

	public ElementView(ComponentType type) {
		super();
		this.type = type;
		element = type.createElement();
	}

	public ComponentType getType() {
		return type;
	}

	public void setType(ComponentType type) {
		this.type = type;
	}

	public Tool getElementTool() {
		return elementTool;
	}

	public void setElementTool(Tool elementTool) {
		this.elementTool = elementTool;
	}

	public Node getLabel() {
		return label;
	}

	public void setLabel(Node label) {
		this.label = label;
	}

	public IO getElement() {
		return element;
	}

	public void setElement(IO element) {
		this.element = element;
	}

	@Override
	public String toString() {
		return "ElementView [type=" + type + ", elementTool=" + elementTool + ", label=" + label + ", element="
				+ element + "]";
	}

	public void redraw() {
		elementTool.redraw();
	}

	public void reset() {
		elementTool.reset();
	}

}