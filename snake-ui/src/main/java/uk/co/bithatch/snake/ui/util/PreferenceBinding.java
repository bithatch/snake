package uk.co.bithatch.snake.ui.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import javafx.beans.property.Property;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

public class PreferenceBinding implements Closeable, PreferenceChangeListener {

	abstract class Binding<T> implements ChangeListener<T> {
		Property<T> property;
		String key;

		public Binding(Property<T> property, String key) {
			super();
			this.property = property;
			this.key = key;
			property.addListener(this);
		}

		public void unbind() {
			property.removeListener(this);
		}

		abstract void changed(String newValue);
	}

	private Preferences node;
	private Map<String, Binding<?>> map = new HashMap<>();

	public PreferenceBinding(Preferences node) {
		this.node = node;
		node.addPreferenceChangeListener(this);
	}

	public void bind(String key, Property<Boolean> property, boolean defaultValue) {
		boolean value = node.getBoolean(key, defaultValue);
		property.setValue(value);
		var binding = new Binding<Boolean>(property, key) {
			@Override
			public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
				node.putBoolean(key, newValue);
			}

			@Override
			void changed(String newValue) {
				property.setValue(Boolean.valueOf(newValue));
			}
		};
		map.put(key, binding);
	}

	@Override
	public void close() throws IOException {
		for (Iterator<Map.Entry<String, Binding<?>>> it = map.entrySet().iterator(); it.hasNext();) {
			Map.Entry<String, Binding<?>> binding = it.next();
			binding.getValue().unbind();
			it.remove();
		}
		node.removePreferenceChangeListener(this);
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent evt) {
		Binding<?> b = map.get(evt.getKey());
		b.changed(evt.getNewValue());
	}
}
