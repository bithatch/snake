package uk.co.bithatch.snake.ui.designer;

import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import uk.co.bithatch.snake.lib.layouts.ComponentType;

public interface Viewer {

	public interface ViewerListener {
		void viewerSelected(ViewerView view);
	}

	void addListener(ViewerListener listener);

	void removeListener(ViewerListener listener);

	void removeSelectedElements();

	List<ComponentType> getEnabledTypes();

	void setEnabledTypes(List<ComponentType> enabledTypes);

	SimpleBooleanProperty selectableElements();

	SimpleBooleanProperty readOnly();

	default void setReadOnly(boolean readOnly) {
		readOnly().set(readOnly);
	}

	default boolean isReadOnly() {
		return readOnly().get();
	}

	default void setSelectableElements(boolean selectableElements) {
		selectableElements().set(selectableElements);
	}

	default boolean isSelectableElements() {
		return selectableElements().get();
	}
}
