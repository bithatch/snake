package uk.co.bithatch.snake.ui;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.prefs.Preferences;

import com.sshtools.icongenerator.AwesomeIcon;
import com.sshtools.icongenerator.IconBuilder;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import uk.co.bithatch.snake.lib.Key;
import uk.co.bithatch.snake.lib.Macro;
import uk.co.bithatch.snake.lib.MacroKey;
import uk.co.bithatch.snake.lib.MacroKey.State;
import uk.co.bithatch.snake.lib.MacroScript;
import uk.co.bithatch.snake.lib.MacroSequence;
import uk.co.bithatch.snake.lib.MacroURL;
import uk.co.bithatch.snake.lib.ValidationException;
import uk.co.bithatch.snake.ui.SlideyStack.Direction;

public class Macros extends AbstractDetailsController {

	public static IconBuilder builderForMacroSequence(MacroSequence seq) {
		IconBuilder builder = new IconBuilder();
		builder.width(32);
		builder.height(32);
		builder.round();
		var txt = seq.getMacroKey().name();
		if (txt.length() > 3)
			txt = txt.substring(0, 3);
		builder.text(txt);
		builder.color(Integer.parseInt("ff00ff", 16));
		builder.autoTextColor();
		return builder;
	}

	public static IconBuilder builderForMacro(Macro mkey) {
		IconBuilder builder = new IconBuilder();
		builder.width(24);
		builder.height(24);
		builder.roundRect(4);
		if (mkey instanceof MacroURL) {
			builder.color(Integer.parseInt("ffff00", 16));
			builder.textColor(0);
			builder.icon(AwesomeIcon.GLOBE);
		} else if (mkey instanceof MacroScript) {
			builder.color(Integer.parseInt("00ffff", 16));
			builder.textColor(0);
			builder.icon(AwesomeIcon.HASHTAG);
		} else {
			builder.color(Integer.parseInt("00ff00", 16));
			builder.textColor(0);
			builder.icon(AwesomeIcon.KEYBOARD_O);
		}
		builder.fontSize(24);
		builder.bold(true);
		return builder;
	}

	private static class MacroListCell extends TreeCell<Object> {
		@Override
		public void updateItem(Object item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setGraphic(null);
				setText(null);
			} else {
				IconBuilder builder;
				if (item instanceof MacroSequence) {
					MacroSequence seq = (MacroSequence) item;
					builder = builderForMacroSequence(seq);
					setText(seq.getMacroKey().name());
				} else {
					Macro macro = (Macro) item;
					builder = builderForMacro(macro);
					if (macro == null)
						setText("<null>");
					else
						setText(bundle.getString("macroType." + macro.getClass().getSimpleName()));
				}

				var icon = builder.build(Canvas.class);
				setGraphic(icon);
			}
		}
	}

	final static ResourceBundle bundle = ResourceBundle.getBundle(Macros.class.getName());

	final static Preferences PREFS = Preferences.userNodeForPackage(Macros.class);

	static List<String> parseQuotedString(String command) {
		List<String> args = new ArrayList<String>();
		boolean escaped = false;
		boolean quoted = false;
		StringBuilder word = new StringBuilder();
		for (int i = 0; i < command.length(); i++) {
			char c = command.charAt(i);
			if (c == '"' && !escaped) {
				if (quoted) {
					quoted = false;
				} else {
					quoted = true;
				}
			} else if (c == '\\' && !escaped) {
				escaped = true;
			} else if ((c == ' ' || c == '\n') && !escaped && !quoted) {
				if (word.length() > 0) {
					args.add(word.toString());
					word.setLength(0);
					;
				}
			} else {
				word.append(c);
			}
		}
		if (word.length() > 0)
			args.add(word.toString());
		return args;
	}

	@FXML
	private Hyperlink add;
	private boolean adjusting = false;
	@FXML
	private Hyperlink delete;
	@FXML
	private VBox editor;
	@FXML
	private Label error;
	@FXML
	private Hyperlink export;
	@FXML
	private RadioButton keyMacro;
	@FXML
	private VBox keyMacroSection;
	@FXML
	private ComboBox<Key> macroKey;
	@FXML
	private TreeView<Object> macros;
	private Set<MacroSequence> macrosToSave = new LinkedHashSet<>();
	@FXML
	private ToggleGroup macroType;
	@FXML
	private TextField pause;
	@FXML
	private TextArea scriptArgs;
	@FXML
	private Button scriptBrowse;
	@FXML
	private TextField scriptLocation;
	@FXML
	private RadioButton scriptMacro;
	@FXML
	private VBox scriptMacroSection;

	@FXML
	private VBox sequenceEditor;

	@FXML
	private ComboBox<Key> simulateKey;
	@FXML
	private ComboBox<State> state;

	private ScheduledFuture<?> task;

	@FXML
	private TextField urlLocation;
	@FXML
	private RadioButton urlMacro;
	@FXML
	private VBox urlMacroSection;

	@FXML
	private Button urlOpen;

	@FXML
	private Hyperlink recordMacro;

	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	protected void error(String key) {
		error.visibleProperty().set(key != null);
		if (key == null) {
			error.visibleProperty().set(false);
			error.textProperty().set("");
		} else {
			error.visibleProperty().set(true);
			String txt = bundle.getString(key);
			error.textProperty().set(bundle.getString("errorIcon") + (txt == null ? "<missing key " + key + ">" : txt));
		}
	}

	@SuppressWarnings("unchecked")
	protected <M extends Macro> M getSelectedMacro() {
		Object sel = macros.getSelectionModel().isEmpty() ? null
				: macros.getSelectionModel().getSelectedItem().getValue();
		if (sel instanceof Macro)
			return (M) macros.getSelectionModel().getSelectedItem().getValue();
		else
			return null;
	}

	protected MacroSequence getSelectedSequence() {
		Object sel = macros.getSelectionModel().isEmpty() ? null
				: macros.getSelectionModel().getSelectedItem().getValue();
		if (sel instanceof MacroSequence)
			return (MacroSequence) sel;
		else if (sel instanceof Macro)
			return (MacroSequence) macros.getSelectionModel().getSelectedItem().getParent().getValue();
		else
			return null;
	}

	protected TreeItem<Object> getSelectedSequenceItem() {
		TreeItem<Object> sel = macros.getSelectionModel().isEmpty() ? null
				: macros.getSelectionModel().getSelectedItem();
		if (sel != null && sel.getValue() instanceof MacroSequence)
			return sel;
		else if (sel != null && sel.getParent().getValue() instanceof MacroSequence)
			return sel.getParent();
		else
			return null;
	}

	@Override
	protected void onCleanUp() {
		if (LOG.isLoggable(Level.DEBUG))
			LOG.log(Level.DEBUG, "Stopping macros scheduler.");
		executor.shutdown();
	}

	@Override
	protected void onSetDeviceDetails() throws Exception {
		keyMacro.selectedProperty().set(true);
		urlMacroSection.managedProperty().bind(urlMacroSection.visibleProperty());
		urlMacroSection.visibleProperty().bind(urlMacro.selectedProperty());
		scriptMacroSection.managedProperty().bind(scriptMacroSection.visibleProperty());
		scriptMacroSection.visibleProperty().bind(scriptMacro.selectedProperty());
		keyMacroSection.managedProperty().bind(keyMacroSection.visibleProperty());
		keyMacroSection.visibleProperty().bind(keyMacro.selectedProperty());
		var keyList = new ArrayList<>(Arrays.asList(Key.values()));
		Collections.sort(keyList, (k1, k2) -> k1.name().compareTo(k2.name()));
		macroKey.itemsProperty().get().addAll(keyList);
		state.itemsProperty().get().addAll(Arrays.asList(State.values()));
		UnaryOperator<Change> integerFilter = change -> {
			String newText = change.getControlNewText();
			if (newText.matches("-?([1-9][0-9]*)?")) {
				return change;
			}
			return null;
		};
		pause.setTextFormatter(new TextFormatter<Integer>(new IntegerStringConverter(), 0, integerFilter));
		simulateKey.itemsProperty().get().addAll(keyList);
		editor.managedProperty().bind(editor.visibleProperty());
		urlOpen.disableProperty().bind(Bindings.isEmpty(urlLocation.textProperty()));
		scriptLocation.textProperty().addListener((e) -> {
			var macro = (MacroScript) getSelectedMacro();
			macro.setScript(scriptLocation.textProperty().get());
			saveMacroSequence(getSelectedSequence());
		});
		state.getSelectionModel().selectedItemProperty().addListener((e) -> {
			var macro = (MacroKey) getSelectedMacro();
			if (macro != null) {
				macro.setState(state.getSelectionModel().getSelectedItem());
				saveMacroSequence(getSelectedSequence());
			}
		});
		pause.textProperty().addListener((e) -> {
			var macro = (MacroKey) getSelectedMacro();
			try {
				macro.setPrePause(Long.parseLong(pause.textProperty().get()));
				saveMacroSequence(getSelectedSequence());
			} catch (NumberFormatException nfe) {
				error("invalidPause");
			}
		});
		urlLocation.textProperty().addListener((e) -> {
			var macro = (MacroURL) getSelectedMacro();
			macro.setUrl(urlLocation.textProperty().get());
			saveMacroSequence(getSelectedSequence());
		});
		scriptArgs.textProperty().addListener((e) -> {
			var macro = (MacroScript) getSelectedMacro();
			macro.setArgs(parseQuotedString(urlLocation.textProperty().get()));
			saveMacroSequence(getSelectedSequence());
		});
		macroKey.getSelectionModel().selectedItemProperty().addListener((e) -> {
			var macro = getSelectedSequence();
			if (macro != null) {
				macro.setMacroKey(macroKey.getSelectionModel().getSelectedItem());
				saveMacroSequence(macro);
				macros.refresh();
			}
		});
		macros.getSelectionModel().selectedItemProperty().addListener((e) -> {
			if (!adjusting) {
				var macro = getSelectedMacro();
				editor.visibleProperty().set(macro != null);
				if (macro == null) {
					delete.textProperty().set(bundle.getString("deleteSequence"));
				} else {
					delete.textProperty().set(bundle.getString("delete"));
				}
				setSequence(getSelectedSequence());
				setMacro(macro);
				setAvailableEditors();
				if (macro instanceof MacroURL)
					urlMacro.selectedProperty().set(true);
				else if (macro instanceof MacroScript)
					scriptMacro.selectedProperty().set(true);
				else
					keyMacro.selectedProperty().set(true);
			}
		});
		delete.visibleProperty().set(getSelectedSequence() != null);
		sequenceEditor.visibleProperty().set(getSelectedSequence() != null);
		editor.visibleProperty().set(getSelectedMacro() != null);
		macros.setCellFactory((list) -> {
			return new MacroListCell();
		});
		TreeItem<Object> root = new TreeItem<>();
		macros.rootProperty().set(root);
		buildTree();
		macros.setShowRoot(false);
		urlMacro.selectedProperty().addListener((e) -> {
			if (urlMacro.selectedProperty().get()) {
				adjusting = true;
				try {
					TreeItem<Object> seqItem = macros.getSelectionModel().getSelectedItem().getParent();
					MacroSequence seq = getSelectedSequence();
					Macro macro = getSelectedMacro();
					MacroURL murl = new MacroURL();
					var idx = seq.indexOf(macro);
					seq.set(idx, murl);
					TreeItem<Object> newMacroItem = new TreeItem<>(murl);
					seqItem.getChildren().set(idx, newMacroItem);
					saveMacroSequence(seq);
					macros.getSelectionModel().select(newMacroItem);
				} finally {
					adjusting = false;
				}
			}
		});
		scriptMacro.selectedProperty().addListener((e) -> {
			if (scriptMacro.selectedProperty().get()) {
				adjusting = true;
				try {
					TreeItem<Object> seqItem = macros.getSelectionModel().getSelectedItem().getParent();
					MacroSequence seq = getSelectedSequence();

					Macro macro = getSelectedMacro();
					MacroScript murl = new MacroScript();
					var idx = seq.indexOf(macro);
					seq.set(idx, murl);
					TreeItem<Object> newMacroItem = new TreeItem<>(murl);
					seqItem.getChildren().set(idx, newMacroItem);
					saveMacroSequence(seq);
					macros.getSelectionModel().select(newMacroItem);
				} finally {
					adjusting = false;
				}
			}
		});
		keyMacro.selectedProperty().addListener((e) -> {
			if (keyMacro.selectedProperty().get()) {
				adjusting = true;
				try {
					TreeItem<Object> seqItem = macros.getSelectionModel().getSelectedItem().getParent();
					MacroSequence seq = getSelectedSequence();
					Macro macro = getSelectedMacro();
					if (macro != null) {
						MacroKey murl = new MacroKey();
						var idx = seq.indexOf(macro);
						seq.set(idx, murl);
						TreeItem<Object> newMacroItem = new TreeItem<>(murl);
						seqItem.getChildren().set(idx, newMacroItem);
						saveMacroSequence(seq);
						macros.getSelectionModel().select(newMacroItem);
					}
				} finally {
					adjusting = false;
				}
			}
		});
		export.visibleProperty().bind(Bindings.isNotEmpty(macros.rootProperty().getValue().getChildren()));
	}

	private void buildTree() {
		var root = macros.rootProperty().get();
		for (Map.Entry<Key, MacroSequence> en : getDevice().getMacros().entrySet()) {
			TreeItem<Object> macroSequence = new TreeItem<>(en.getValue());
			for (Macro m : en.getValue())
				macroSequence.getChildren().add(new TreeItem<>(m));
			root.getChildren().add(macroSequence);
			macroSequence.setExpanded(en.getValue().size() < 3);
		}
	}

	@FXML
	void evtAdd(ActionEvent evt) {
		Map<Key, MacroSequence> existing = getDevice().getMacros();
		for (Key k : Key.values()) {
			if (!existing.containsKey(k)) {
				MacroSequence m = new MacroSequence();
				m.setMacroKey(k);
				var item = new TreeItem<Object>(m);
				macros.rootProperty().get().getChildren().add(item);
				macros.getSelectionModel().select(item);
				return;
			}
		}
		error("noKeysLeft");
	}

	@FXML
	void evtAddMacro(ActionEvent evt) {
		var mk = new MacroKey();
		var seq = getSelectedSequence();
		seq.add(mk);
		TreeItem<Object> t = new TreeItem<>(mk);
		TreeItem<Object> seqItem = getSelectedSequenceItem();
		seqItem.setExpanded(true);
		seqItem.getChildren().add(t);
		saveMacroSequence(seq);
		macros.getSelectionModel().select(t);
	}

	@FXML
	void evtRecordMacro(ActionEvent evt) {
		context.push(Record.class, Direction.FROM_LEFT).setMacroSequence(getSelectedSequence());
	}

	@FXML
	void evtDelete(ActionEvent evt) {
		Macro m = getSelectedMacro();
		var seq = getSelectedSequence();
		if (m == null)
			getDevice().deleteMacro(seq.getMacroKey());
		else {
			seq.remove(m);
			saveMacroSequence(seq);
		}
		macros.getSelectionModel().getSelectedItem().getParent().getChildren()
				.remove(macros.getSelectionModel().getSelectedItem());
		setAvailableEditors();
	}

	@FXML
	void evtExport(ActionEvent evt) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("selectExportFile"));
		var path = PREFS.get("lastExportLocation", System.getProperty("user.dir") + File.separator + "macros.json");
		fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("macroFileExtension"), "*.json"));
		fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("allFiles"), "*.*"));
		UIHelpers.selectFilesDir(fileChooser, path);
		File file = fileChooser.showSaveDialog((Stage) getScene().getWindow());
		if (file != null) {
			PREFS.put("lastExportLocation", file.getAbsolutePath());
			try (PrintWriter pw = new PrintWriter(file)) {
				pw.println(getDevice().exportMacros());
			} catch (IOException ioe) {
				LOG.log(java.lang.System.Logger.Level.ERROR, "Failed to export macros.", ioe);
			}
		}
	}

	@FXML
	void evtImport(ActionEvent evt) {

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("selectImportFile"));
		var path = PREFS.get("lastExportLocation", System.getProperty("user.dir") + File.separator + "macros.json");
		fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("macroFileExtension"), "*.json"));
		fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("allFiles"), "*.*"));
		UIHelpers.selectFilesDir(fileChooser, path);
		File file = fileChooser.showOpenDialog((Stage) getScene().getWindow());
		if (file != null) {
			PREFS.put("lastExportLocation", file.getAbsolutePath());
			try {
				getDevice().importMacros(String.join(" ", Files.readAllLines(file.toPath())));
				macros.rootProperty().get().getChildren().clear();
				buildTree();
			} catch (IOException ioe) {
				LOG.log(java.lang.System.Logger.Level.ERROR, "Failed to export macros.", ioe);
			}
		}
	}

	@FXML
	void evtScriptBrowse(ActionEvent evt) {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("selectExecutable"));
		var path = scriptLocation.textProperty().get();
		if (path == null || path.equals(""))
			path = System.getProperty("user.dir");
		UIHelpers.selectFilesDir(fileChooser, path);
		File file = fileChooser.showOpenDialog((Stage) getScene().getWindow());
		if (file != null) {
			scriptLocation.textProperty().set(file.getPath());
			var macro = (MacroScript) getSelectedMacro();
			macro.setScript(scriptLocation.textProperty().get());
			saveMacroSequence(getSelectedSequence());
		}
	}

	@FXML
	void evtUrlOpen(ActionEvent evt) {
		context.getHostServices().showDocument(urlLocation.textProperty().get());
	}

	private void saveMacroSequence(MacroSequence mkey) {
		try {
			mkey.validate();

			if (task != null) {
				task.cancel(false);
			}
			error(null);
			synchronized (macrosToSave) {
				macrosToSave.add(mkey);
			}
			task = executor.schedule(() -> {
				synchronized (macrosToSave) {
					try {
						for (MacroSequence m : macrosToSave) {
							getDevice().deleteMacro(m.getMacroKey());
							getDevice().addMacro(m);
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						macrosToSave.clear();
					}
				}
			}, 1000, TimeUnit.MILLISECONDS);
		} catch (ValidationException ve) {
			synchronized (macrosToSave) {
				macrosToSave.remove(mkey);
			}
			error(ve.getMessage());
		}
	}

	private void setAvailableEditors() {
		var seq = getSelectedSequence();
		sequenceEditor.visibleProperty().set(seq != null);
		delete.visibleProperty().set(seq != null);
	}

	private void setMacro(Macro macro) {
		if (macro != null) {
			if (macro instanceof MacroKey) {
				MacroKey macroKey = (MacroKey) macro;
				simulateKey.getSelectionModel().select(macroKey.getKey());
				pause.textProperty().set(String.valueOf(macroKey.getPrePause()));
				state.getSelectionModel().select(macroKey.getState());
			} else if (macro instanceof MacroURL) {
				MacroURL macroURL = (MacroURL) macro;
				urlLocation.textProperty().set(macroURL.getUrl());
			} else if (macro instanceof MacroScript) {
				MacroScript macroScript = (MacroScript) macro;
				scriptLocation.textProperty().set(macroScript.getScript());
				if (macroScript.getArgs() == null || macroScript.getArgs().isEmpty())
					scriptArgs.textProperty().set("");
				else
					scriptArgs.textProperty().set(String.join("\n", macroScript.getArgs()));
			}
		}
	}

	private void setSequence(MacroSequence seq) {
		macroKey.getSelectionModel().select(seq == null ? null : seq.getMacroKey());
	}
}
