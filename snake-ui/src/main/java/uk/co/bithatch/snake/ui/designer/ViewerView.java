package uk.co.bithatch.snake.ui.designer;

import java.util.List;

import javafx.scene.Node;
import javafx.scene.control.MultipleSelectionModel;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.layouts.DeviceView;
import uk.co.bithatch.snake.lib.layouts.IO;

public interface ViewerView extends AutoCloseable {
	
	DeviceView getView();

	void open(Device device, DeviceView view, Viewer viewer);

	Node getRoot();
	
	void refresh();

	MultipleSelectionModel<IO> getKeySelectionModel();

	default void updateFromMatrix(int[][][] frame) {
	}

	List<IO> getElements();

}
