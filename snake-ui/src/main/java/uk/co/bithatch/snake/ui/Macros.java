package uk.co.bithatch.snake.ui;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
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

import org.controlsfx.control.SearchableComboBox;

import com.sshtools.icongenerator.AwesomeIcon;
import com.sshtools.icongenerator.IconBuilder.TextContent;

import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
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
import uk.co.bithatch.snake.ui.util.Strings;
import uk.co.bithatch.snake.widgets.GeneratedIcon;
import uk.co.bithatch.snake.widgets.JavaFX;

public class Macros extends AbstractDetailsController {

	public static String textForMapSequence(MacroSequence seq) {
		return textForInputEvent(seq.getMacroKey());
	}

	protected static String textForMapAction(Macro action) {
		if (action instanceof MacroKey)
			return textForInputEvent(((MacroKey) action).getKey());
		else if (action instanceof MacroScript)
			return ((MacroScript) action).getScript();
		else {
			String url = ((MacroURL) action).getUrl();
			try {
				String host = new URL(url).getHost();
				if (host == null || host.equals(""))
					return " ";
				else
					return host;
			} catch (MalformedURLException murle) {
				return " ";
			}
		}
	}

	protected static String textForInputEvent(Key k) {
		String keyName = String.valueOf(k);
		if (keyName.length() > 3)
			return Strings.toName(keyName);
		else {
			if (keyName.startsWith("BTN_")) {
				return MessageFormat.format(bundle.getString("button.name"), keyName);
			} else {
				return MessageFormat.format(bundle.getString("key.name"), keyName);
			}
		}
	}

	public static GeneratedIcon iconForMapSequence(MacroSequence seq) {
		GeneratedIcon icon = new GeneratedIcon();
		icon.getStyleClass().add("mapSequence");
		icon.setPrefHeight(32);
		icon.setPrefWidth(32);
		String keyName = String.valueOf(seq.getMacroKey());
		icon.getStyleClass().add("mapSequenceKey");
		icon.setText(keyName);
		if (keyName.length() > 3)
			icon.setTextContent(TextContent.INITIALS);
		else
			icon.setTextContent(TextContent.ORIGINAL);
		return icon;
	}

	public static GeneratedIcon iconForMacro(Macro mkey) {
		GeneratedIcon icon = new GeneratedIcon();
		icon.getStyleClass().add("mapAction");
		icon.setPrefHeight(24);
		icon.setPrefWidth(24);
		if (mkey instanceof MacroScript) {
			icon.setIcon(AwesomeIcon.HASHTAG);
		} else if (mkey instanceof MacroURL) {
			icon.setIcon(AwesomeIcon.GLOBE);
		} else {
			if (((MacroKey) mkey).getState() == State.DOWN)
				icon.setIcon(AwesomeIcon.ARROW_DOWN);
			else
				icon.setIcon(AwesomeIcon.ARROW_UP);
		}
		return icon;
	}

	private static class MacroListCell extends TreeCell<Object> {
		@Override
		public void updateItem(Object item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setGraphic(null);
				setText(null);
			} else {
				if (item instanceof MacroSequence) {
					MacroSequence seq = (MacroSequence) item;
					setGraphic(iconForMapSequence(seq));
					setText(textForMapSequence(seq));
				} else {
					Macro macro = (Macro) item;
					setGraphic(iconForMacro(macro));
					if (macro == null)
						setText("<null>");
					else
						setText(MessageFormat.format(bundle.getString("macroType." + macro.getClass().getSimpleName()),
								textForMapAction(macro)));
				}

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
	private SearchableComboBox<Key> macroKey;
	@FXML
	private TreeView<Object> macros;
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
	private SearchableComboBox<Key> simulateKey;
	@FXML
	private ComboBox<State> state;

	@FXML
	private TextField urlLocation;
	@FXML
	private RadioButton urlMacro;
	@FXML
	private VBox urlMacroSection;

	@FXML
	private Button urlOpen;

	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> task;
	private Set<MacroSequence> sequencesToSave = new LinkedHashSet<>();
	private boolean adjusting = false;

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
	protected void onDeviceCleanUp() {
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

		if (context.getLayouts().hasLayout(getDevice()))
			macroKey.itemsProperty().get().addAll(context.getLayouts().getLayout(getDevice()).getSupportedLegacyKeys());
		else
			macroKey.itemsProperty().get().addAll(getDevice().getSupportedLegacyKeys());

		state.itemsProperty().get().addAll(Arrays.asList(State.values()));
		UnaryOperator<Change> integerFilter = change -> {
			String newText = change.getControlNewText();
			try {
				Integer.parseInt(newText);
				return change;
			}
			catch(Exception e) {
			}
			return null;
		};
		pause.setTextFormatter(new TextFormatter<Integer>(new IntegerStringConverter(), 0, integerFilter));

		// TODO this is wrong, should be 'xte' events
		simulateKey.itemsProperty().get().addAll(getDevice().getSupportedLegacyKeys());

		editor.managedProperty().bind(editor.visibleProperty());
		urlOpen.disableProperty().bind(Bindings.isEmpty(urlLocation.textProperty()));
		scriptLocation.textProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				var macro = (MacroScript) getSelectedMacro();
				macro.setScript(scriptLocation.textProperty().get());
				saveMacroSequence(getSelectedSequence());
				macros.refresh();
			}
		});
		state.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				var macro = (MacroKey) getSelectedMacro();
				if (macro != null) {
					macro.setState(state.getSelectionModel().getSelectedItem());
					saveMacroSequence(getSelectedSequence());
					macros.refresh();
				}
			}
		});
		simulateKey.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				var macro = (MacroKey) getSelectedMacro();
				if (macro != null) {
					macro.setKey(simulateKey.getSelectionModel().getSelectedItem());
					saveMacroSequence(getSelectedSequence());
				}
			}
		});
		pause.textProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				var macro = (MacroKey) getSelectedMacro();
				try {
					macro.setPrePause(Long.parseLong(pause.textProperty().get()));
					saveMacroSequence(getSelectedSequence());
				} catch (NumberFormatException nfe) {
					error("invalidPause");
				}
			}
		});
		urlLocation.textProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				var macro = (MacroURL) getSelectedMacro();
				macro.setUrl(urlLocation.textProperty().get());
				saveMacroSequence(getSelectedSequence());
				macros.refresh();
			}
		});
		scriptArgs.textProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				var macro = (MacroScript) getSelectedMacro();
				macro.setArgs(parseQuotedString(scriptArgs.textProperty().get()));
				saveMacroSequence(getSelectedSequence());
				macros.refresh();
			}
		});
		macroKey.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting && n != null) {
				var macro = getSelectedSequence();
				if (macro != null) {
					if (o != null)
						getDevice().deleteMacro(o);
					macro.setMacroKey(n);
					saveMacroSequence(macro);
					macros.refresh();
				}
			}
		});
		macros.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				adjusting = true;
				try {
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
				} finally {
					adjusting = false;
				}
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
		urlMacro.selectedProperty().addListener((e, o, n) -> {
			if (!adjusting) {
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
			}
		});
		scriptMacro.selectedProperty().addListener((e, o, n) -> {
			if (!adjusting) {
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
			}
		});
		keyMacro.selectedProperty().addListener((e, o, n) -> {
			if (!adjusting) {
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
	void evtAdd() {
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
	void evtAddMacro() {
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
	void evtDelete() {
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
	void evtExport() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("selectExportFile"));
		var path = PREFS.get("lastExportLocation", System.getProperty("user.dir") + File.separator + "macros.json");
		fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("macroFileExtension"), "*.json"));
		fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("allFiles"), "*.*"));
		JavaFX.selectFilesDir(fileChooser, path);
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
	void evtImport() {

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("selectImportFile"));
		var path = PREFS.get("lastExportLocation", System.getProperty("user.dir") + File.separator + "macros.json");
		fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("macroFileExtension"), "*.json"));
		fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("allFiles"), "*.*"));
		JavaFX.selectFilesDir(fileChooser, path);
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
	void evtScriptBrowse() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("selectExecutable"));
		var path = scriptLocation.textProperty().get();
		if (path == null || path.equals(""))
			path = System.getProperty("user.dir");
		JavaFX.selectFilesDir(fileChooser, path);
		File file = fileChooser.showOpenDialog((Stage) getScene().getWindow());
		if (file != null) {
			scriptLocation.textProperty().set(file.getPath());
			var macro = (MacroScript) getSelectedMacro();
			macro.setScript(scriptLocation.textProperty().get());
			saveMacroSequence(getSelectedSequence());
		}
	}

	@FXML
	void evtUrlOpen() {
		context.getHostServices().showDocument(urlLocation.textProperty().get());
	}

	private void saveMacroSequence(MacroSequence mkey) {
		try {
			mkey.validate();

			if (task != null) {
				task.cancel(false);
			}
			error((String)null);
			synchronized (sequencesToSave) {
				sequencesToSave.add(mkey);
			}
			task = executor.schedule(() -> {
				synchronized (sequencesToSave) {
					try {
						for (MacroSequence m : sequencesToSave) {
							getDevice().deleteMacro(m.getMacroKey());
							getDevice().addMacro(m);
						}
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						sequencesToSave.clear();
						context.getLegacyMacroStorage().save();
					}
				}
			}, 1000, TimeUnit.MILLISECONDS);
		} catch (ValidationException ve) {
			synchronized (sequencesToSave) {
				sequencesToSave.remove(mkey);
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
