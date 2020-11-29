package uk.co.bithatch.snake.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Key;
import uk.co.bithatch.snake.lib.Macro;
import uk.co.bithatch.snake.lib.MacroKey;
import uk.co.bithatch.snake.lib.MacroKey.State;
import uk.co.bithatch.snake.lib.MacroScript;
import uk.co.bithatch.snake.lib.MacroSequence;
import uk.co.bithatch.snake.lib.MacroURL;
import uk.co.bithatch.snake.ui.util.Prefs;

/**
 * This class is thrown together as a temporary solution for macro storage
 * before the new system is fully implemented.
 */
public class LegacyMacroStorage {

	private App context;
	private List<Device> devices = new ArrayList<>();

	public LegacyMacroStorage(App context) {
		this.context = context;
	}

	public void addDevice(Device device) {
		devices.add(device);
		if (device.getMacros().isEmpty()) {
			Preferences prefs = context.getPreferences(device).node("legacyMacros");
			int seqs = prefs.getInt("sequences", 0);
			for (int i = 0; i < seqs; i++) {
				MacroSequence seq = getSequence(prefs.node(String.valueOf(i)));
				if (seq != null)
					device.addMacro(seq);
			}
		}
	}

	public synchronized void save() {
		try {
			for (Device device : devices) {
				Preferences prefs = context.getPreferences(device).node("legacyMacros");
				int currentSeqs = prefs.getInt("sequences", 0);
				for (int i = 0; i < currentSeqs; i++) {
					prefs.node(String.valueOf(i)).removeNode();
				}
				Collection<MacroSequence> sequences = device.getMacros().values();
				prefs.putInt("sequences", sequences.size());
				Iterator<MacroSequence> seqIt = sequences.iterator();
				for (int i = 0; i < sequences.size(); i++) {
					saveSequence(seqIt.next(), prefs.node(String.valueOf(i)));
				}
			}
		} catch (BackingStoreException bse) {
			throw new IllegalStateException("Failed to store macros.", bse);
		}

	}

	private void saveSequence(MacroSequence macroSequence, Preferences node) {
		node.putInt("actions", macroSequence.size());
		node.put("key", macroSequence.getMacroKey().nativeKeyName());
		for (int i = 0; i < macroSequence.size(); i++) {
			saveAction(macroSequence.get(i), node.node(String.valueOf(i)));
		}
	}

	private void saveAction(Macro macro, Preferences node) {
		if (macro instanceof MacroKey) {
			MacroKey mk = (MacroKey) macro;
			node.put("type", "key");
			node.put("state", mk.getState().name());
			node.putLong("pause", mk.getPrePause());
			node.put("key", mk.getKey().name());
		} else if (macro instanceof MacroURL) {
			MacroURL mk = (MacroURL) macro;
			node.put("type", "url");
			node.put("url", mk.getUrl());
		} else if (macro instanceof MacroScript) {
			MacroScript mk = (MacroScript) macro;
			node.put("type", "script");
			node.put("script", mk.getScript());
			Prefs.setStringCollection(node, "args", mk.getArgs());
		} else
			throw new UnsupportedOperationException();
	}

	private MacroSequence getSequence(Preferences node) {
		int actions = node.getInt("actions", 0);
		MacroSequence seq = new MacroSequence();
		try {
			seq.setMacroKey(Key.fromNativeKeyName(node.get("key", "")));
		} catch (IllegalArgumentException iae) {
			return null;
		}
		for (int i = 0; i < actions; i++) {
			seq.add(getMacro(node.node(String.valueOf(i))));
		}
		return seq;
	}

	private Macro getMacro(Preferences node) {
		String type = node.get("type", "");
		if (type.equals("key")) {
			MacroKey mk = new MacroKey();
			mk.setKey(Key.valueOf(node.get("key", "")));
			mk.setPrePause(node.getLong("pause", 0));
			mk.setState(State.valueOf(node.get("state", State.DOWN.name())));
			return mk;
		} else if (type.equals("url")) {
			MacroURL mk = new MacroURL();
			mk.setUrl(node.get("url", ""));
			return mk;
		} else if (type.equals("script")) {
			MacroScript mk = new MacroScript();
			mk.setScript(node.get("script", ""));
			mk.setArgs(Arrays.asList(Prefs.getStringList(node, "args")));
			return mk;
		} else
			throw new UnsupportedOperationException();
	}

}
