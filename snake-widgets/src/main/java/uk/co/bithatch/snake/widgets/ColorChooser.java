package uk.co.bithatch.snake.widgets;

import javafx.beans.property.ObjectProperty;
import javafx.scene.paint.Color;

public interface ColorChooser {

	void revertToOriginalColor();

	void hidePopup();

	void updateHistory();

	Color getColor();

	ObjectProperty<Color> colorProperty();

}
