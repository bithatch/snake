package uk.co.bithatch.snake.ui;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.prefs.Preferences;

import org.apache.commons.lang3.StringUtils;

import com.sshtools.icongenerator.AwesomeIcon;
import com.sshtools.icongenerator.IconBuilder.TextContent;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableListBase;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.converter.FloatStringConverter;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.ValidationException;
import uk.co.bithatch.snake.lib.binding.ExecuteMapAction;
import uk.co.bithatch.snake.lib.binding.KeyMapAction;
import uk.co.bithatch.snake.lib.binding.MapAction;
import uk.co.bithatch.snake.lib.binding.MapMapAction;
import uk.co.bithatch.snake.lib.binding.MapSequence;
import uk.co.bithatch.snake.lib.binding.Profile;
import uk.co.bithatch.snake.lib.binding.ProfileMap;
import uk.co.bithatch.snake.lib.binding.ProfileMapAction;
import uk.co.bithatch.snake.lib.binding.ReleaseMapAction;
import uk.co.bithatch.snake.lib.binding.ShiftMapAction;
import uk.co.bithatch.snake.lib.binding.SleepMapAction;
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.DeviceLayout;
import uk.co.bithatch.snake.lib.layouts.DeviceView;
import uk.co.bithatch.snake.lib.layouts.IO;
import uk.co.bithatch.snake.lib.layouts.Key;
import uk.co.bithatch.snake.ui.designer.LayoutEditor;
import uk.co.bithatch.snake.ui.designer.Viewer;
import uk.co.bithatch.snake.ui.util.BasicList;
import uk.co.bithatch.snake.ui.util.Strings;
import uk.co.bithatch.snake.widgets.Direction;
import uk.co.bithatch.snake.widgets.GeneratedIcon;
import uk.co.bithatch.snake.widgets.JavaFX;

public class MacroMap extends AbstractDetailsController {

	public static String textForMapSequence(MapSequence seq) {
		return textForInputEvent(seq.getMacroKey());
	}

	protected static String textForMapAction(MapAction action) {
		if (action instanceof KeyMapAction)
			return textForInputEvent(((KeyMapAction) action).getPress());
		else if (action instanceof ReleaseMapAction)
			return textForInputEvent(((ReleaseMapAction) action).getRelease());
		else {
			return action.getValue();
		}
	}

	protected static String textForInputEvent(EventCode k) {
		String keyName = String.valueOf(k);
		String txt = keyName.substring(4);
		if (txt.length() > 3)
			return Strings.toName(txt);
		else {
			if (keyName.startsWith("BTN_")) {
				return MessageFormat.format(bundle.getString("button.name"), txt);
			} else {
				return MessageFormat.format(bundle.getString("key.name"), txt);
			}
		}
	}

	public static GeneratedIcon iconForMapSequence(MapSequence seq) {
		GeneratedIcon icon = new GeneratedIcon();
		icon.getStyleClass().add("mapSequence");
		icon.setPrefHeight(32);
		icon.setPrefWidth(32);
		String keyName = String.valueOf(seq.getMacroKey());
		if (keyName.startsWith("BTN_")) {
			icon.getStyleClass().add("mapSequenceButton");
		} else {
			icon.getStyleClass().add("mapSequenceKey");
		}
		String txt = keyName.substring(4);
		icon.setText(txt);
		if (txt.length() > 3)
			icon.setTextContent(TextContent.INITIALS);
		else
			icon.setTextContent(TextContent.ORIGINAL);
		return icon;
	}

	public static GeneratedIcon iconForMacro(MapAction mkey) {
		GeneratedIcon icon = new GeneratedIcon();
		icon.getStyleClass().add("mapAction");
		icon.setPrefHeight(24);
		icon.setPrefWidth(24);
		if (mkey instanceof SleepMapAction) {
			icon.setIcon(AwesomeIcon.CLOCK_O);
		} else if (mkey instanceof ExecuteMapAction) {
			icon.setIcon(AwesomeIcon.HASHTAG);
		} else if (mkey instanceof ShiftMapAction) {
			icon.setIcon(AwesomeIcon.LONG_ARROW_UP);
		} else if (mkey instanceof MapMapAction) {
			icon.setIcon(AwesomeIcon.GLOBE);
		} else if (mkey instanceof ProfileMapAction) {
			icon.setIcon(AwesomeIcon.ADDRESS_BOOK);
		} else if (mkey instanceof KeyMapAction) {
			icon.setIcon(AwesomeIcon.ARROW_DOWN);
		} else {
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
				if (item instanceof MapSequence) {
					MapSequence seq = (MapSequence) item;
					setGraphic(iconForMapSequence(seq));
					setText(textForMapSequence(seq));
				} else {
					MapAction macro = (MapAction) item;
					setGraphic(iconForMacro(macro));
					if (macro == null)
						setText("<null>");
					else
						setText(MessageFormat.format(
								bundle.getString("mapAction." + macro.getActionType().getSimpleName()),
								textForMapAction(macro)));
				}

			}
		}
	}

	final static ResourceBundle bundle = ResourceBundle.getBundle(MacroMap.class.getName());

	final static Preferences PREFS = Preferences.userNodeForPackage(MacroMap.class);

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
	private Hyperlink export;
	@FXML
	private VBox keySection;
	@FXML
	private ComboBox<EventCode> macroKey;
	@FXML
	private ToggleGroup macroType;
	@FXML
	private TextField seconds;
	@FXML
	private TextArea commandArgs;
	@FXML
	private Button commandBrowse;
	@FXML
	private TextField commandLocation;
	@FXML
	private VBox commandSection;
	@FXML
	private VBox sleepSection;
	@FXML
	private VBox mapSection;
	@FXML
	private VBox profileSection;
	@FXML
	private VBox sequenceEditor;
	@FXML
	private ComboBox<EventCode> keyCode;
	@FXML
	private RadioButton keyMapAction;
	@FXML
	private RadioButton executeMapAction;
	@FXML
	private RadioButton releaseMapAction;
	@FXML
	private RadioButton sleepMapAction;
	@FXML
	private RadioButton shiftMapAction;
	@FXML
	private RadioButton profileMapAction;
	@FXML
	private RadioButton mapMapAction;
	@FXML
	private Hyperlink recordMacro;
	@FXML
	private Hyperlink addMapAction;
	@FXML
	private ComboBox<String> targetMap;
	@FXML
	private ComboBox<String> targetProfile;
	@FXML
	private Label shiftMapActionLabel;
	@FXML
	private Label profileMapActionLabel;
	@FXML
	private Label mapMapActionLabel;
	@FXML
	private BorderPane macrosContainer;

	private Set<MapAction> macrosToSave = new LinkedHashSet<>();
	private Set<MapSequence> sequencesToSave = new LinkedHashSet<>();
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
	private ProfileMap map;
	private ScheduledFuture<?> task;
	private boolean adjusting = false;
	private MacrosView macros;
	private LayoutEditor layoutEditor;

	protected void error(String key) {
		if (key == null)
			clearNotifications(false);
		else {
			String txt = bundle.getString(key);
			notifyMessage(MessageType.DANGER, txt == null ? "<missing key " + key + ">" : txt);
		}
	}

	@SuppressWarnings("unchecked")
	protected <M extends MapAction> M getSelectedMapAction() {
		return (M) macros.getActionSelectionModel().getSelectedItem();
	}

	protected MapSequence getSelectedSequence() {
		return macros.getSequenceSelectionModel().getSelectedItem();
	}

	@Override
	protected void onDeviceCleanUp() {
		if (LOG.isLoggable(Level.DEBUG))
			LOG.log(Level.DEBUG, "Stopping macros scheduler.");
		executor.shutdown();
		if (layoutEditor != null) {
			try {
				layoutEditor.close();
			} catch (Exception e) {
			}
		}
	}

	public void setMap(ProfileMap map) throws Exception {
		this.map = map;
		Device device = map.getProfile().getDevice();

		/*
		 * If the device has a layout with some Key's, then replace the tree view with
		 * the layout view
		 */
		if (context.getLayouts().hasLayout(device)) {
			DeviceLayout layout = context.getLayouts().getLayout(device);
			DeviceView view = layout.getViewThatHas(ComponentType.KEY);
			if (view != null) {
				macros = new LayoutMacrosView(map, view, context);
			}
			keyCode.itemsProperty().get().addAll(layout.getSupportedInputEvents());
		} else {
			/* No layout, so we must present all possible evdev input codes */
			keyCode.itemsProperty().get().addAll(getDevice().getSupportedInputEvents());
		}
		if (macros == null) {
			macros = new TreeMacrosView(map, context);
		}
		macrosContainer.setCenter(macros.getNode());

		JavaFX.bindManagedToVisible(commandSection, keySection, sleepSection, editor, mapSection, profileSection,
				mapMapAction, profileMapAction, shiftMapAction, profileMapActionLabel, mapMapActionLabel,
				shiftMapActionLabel, addMapAction, recordMacro);

		keyMapAction.selectedProperty().set(true);

		profileMapActionLabel.visibleProperty().bind(profileMapAction.visibleProperty());
		mapMapActionLabel.visibleProperty().bind(mapMapAction.visibleProperty());
		shiftMapActionLabel.visibleProperty().bind(shiftMapAction.visibleProperty());
		commandSection.visibleProperty().bind(executeMapAction.selectedProperty());
		sleepSection.visibleProperty().bind(sleepMapAction.selectedProperty());
		mapSection.visibleProperty()
				.bind(Bindings.or(mapMapAction.selectedProperty(), shiftMapAction.selectedProperty()));
		profileSection.visibleProperty().bind(profileMapAction.selectedProperty());
		keySection.visibleProperty()
				.bind(Bindings.or(keyMapAction.selectedProperty(), releaseMapAction.selectedProperty()));

		/* Populate maps and profiles */
		for (Profile profile : getDevice().getProfiles()) {
			if (profile.equals(map.getProfile())) {
				for (ProfileMap profileMap : profile.getMaps()) {
					if (!profileMap.equals(map))
						targetMap.getItems().add(map.getId());
				}
			} else
				targetProfile.getItems().add(profile.getName());
		}

		shiftMapAction.visibleProperty().set(!targetMap.getItems().isEmpty());
		mapMapAction.visibleProperty().set(!targetMap.getItems().isEmpty());
		profileMapAction.visibleProperty().set(!targetProfile.getItems().isEmpty());

		// TODO just show the input event codes supported
		macroKey.itemsProperty().get().addAll(getDevice().getSupportedInputEvents());

		UnaryOperator<Change> floatFilter = change -> {
			String newText = change.getControlNewText();
			if (newText.matches("[-+]?([0-9]*\\.[0-9]+|[0-9]+)")) {
				return change;
			}
			return null;
		};
		seconds.setTextFormatter(new TextFormatter<Float>(new FloatStringConverter(), 0f, floatFilter));

		keyCode.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				var macro = getSelectedMapAction();
				if (macro instanceof KeyMapAction) {
					((KeyMapAction) macro).setPress(n);
				} else if (macro instanceof ReleaseMapAction) {
					((ReleaseMapAction) macro).setRelease(n);
				}
				saveMacroKey(macro);
				macros.refresh();
			}
		});
		commandLocation.textProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				var macro = (ExecuteMapAction) getSelectedMapAction();
				macro.setCommand(commandLocation.textProperty().get());
				saveMacroKey(macro);
			}
		});
		seconds.textProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				var macro = (SleepMapAction) getSelectedMapAction();
				try {
					macro.setSeconds(Float.parseFloat(seconds.textProperty().get()));
					saveMacroKey(macro);
				} catch (NumberFormatException nfe) {
					error("invalidPause");
				}
			}
		});
		commandArgs.textProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				var macro = (ExecuteMapAction) getSelectedMapAction();
				macro.setArgs(parseQuotedString(commandArgs.textProperty().get()));
				saveMacroKey(macro);
			}
		});
		macroKey.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				var seq = getSelectedSequence();
				if (seq != null) {
					seq.setMacroKey(macroKey.getSelectionModel().getSelectedItem());
					saveMacroSequence(seq);
					macros.refresh();
				}
			}
		});
		macros.getSequenceSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				adjusting = true;
				try {
					updateForSequence(getSelectedSequence());
					updateForAction(getSelectedMapAction());
				} finally {
					adjusting = false;
				}
			}
		});
		macros.getActionSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				adjusting = true;
				try {
					updateForAction(getSelectedMapAction());
				} finally {
					adjusting = false;
				}
			}
		});
		delete.visibleProperty().set(getSelectedSequence() != null);
		sequenceEditor.visibleProperty().set(getSelectedSequence() != null);
		editor.visibleProperty().set(getSelectedMapAction() != null);

		macros.buildTree();

		executeMapAction.selectedProperty().addListener(createMapActionListener(ExecuteMapAction.class, () -> ""));
		keyMapAction.selectedProperty().addListener(createMapActionListener(KeyMapAction.class, () -> {
			if (getSelectedMapAction() instanceof ReleaseMapAction) {
				return ((ReleaseMapAction) getSelectedMapAction()).getRelease();
			}
			return EventCode.KEY_1;

		}));
		releaseMapAction.selectedProperty().addListener(createMapActionListener(ReleaseMapAction.class, () -> {
			if (getSelectedMapAction() instanceof KeyMapAction) {
				return ((KeyMapAction) getSelectedMapAction()).getPress();
			}
			return EventCode.KEY_1;

		}));
		shiftMapAction.selectedProperty().addListener(createMapActionListener(ShiftMapAction.class, () -> {
			if (getSelectedMapAction() instanceof KeyMapAction) {
				return ((KeyMapAction) getSelectedMapAction()).getPress();
			} else if (getSelectedMapAction() instanceof ReleaseMapAction) {
				return ((ReleaseMapAction) getSelectedMapAction()).getRelease();
			}
			return EventCode.KEY_1;

		}));

		mapMapAction.selectedProperty().addListener(
				createMapActionListener(MapMapAction.class, () -> targetMap.getSelectionModel().getSelectedItem()));
		profileMapAction.selectedProperty().addListener(createMapActionListener(ProfileMapAction.class,
				() -> targetProfile.getSelectionModel().getSelectedItem()));
		sleepMapAction.selectedProperty().addListener(
				createMapActionListener(SleepMapAction.class, () -> targetMap.getSelectionModel().getSelectedItem()));

		updateState();
		updateForSequence(getSelectedSequence());
		updateForAction(getSelectedMapAction());
	}

	protected void updateForAction(MapAction macro) {
		updateAction(macro);
		updateState();
		if (macro instanceof KeyMapAction)
			keyMapAction.selectedProperty().set(true);
		else if (macro instanceof ReleaseMapAction)
			releaseMapAction.selectedProperty().set(true);
		else if (macro instanceof SleepMapAction)
			sleepMapAction.selectedProperty().set(true);
		else if (macro instanceof ExecuteMapAction)
			executeMapAction.selectedProperty().set(true);
		else if (macro instanceof ShiftMapAction)
			shiftMapAction.selectedProperty().set(true);
		else if (macro instanceof MapMapAction)
			mapMapAction.selectedProperty().set(true);
		else if (macro instanceof ProfileMapAction)
			profileMapAction.selectedProperty().set(true);
		else if (macro != null)
			throw new UnsupportedOperationException("Unknown action type.");
	}

	protected <A extends MapAction> ChangeListener<? super Boolean> createMapActionListener(Class<A> clazz,
			Callable<Object> defaultValue) {
		return (e, o, n) -> {
			if (!adjusting && n) {
				adjusting = true;
				try {
					MapSequence seq = getSelectedSequence();
					MapAction macro = getSelectedMapAction();
					macros.add(seq, macro, clazz, defaultValue);
				} catch (Exception e1) {
					throw new IllegalStateException("Failed to change action type.", e1);
				} finally {
					adjusting = false;
				}
			}
		};
	}

	@FXML
	void evtAdd() {
		try {
			EventCode k = map.getNextFreeKey();
			adjusting = true;
			try {
				MapSequence seq = map.addSequence(k, true);
				macros.addSequence(seq);
				updateForSequence(seq);
				updateForAction(seq.get(0));
				return;
			} finally {
				adjusting = false;
			}
		} catch (IllegalStateException e) {
			error("noKeysLeft");
		}

	}

	@FXML
	void evtAddMapAction() {
		var seq = getSelectedSequence();
		var action = getSelectedMapAction();
		MapAction mk;
		if (action instanceof KeyMapAction) {
			KeyMapAction currentKeyMapAction = (KeyMapAction) action;
			mk = seq.addAction(ReleaseMapAction.class, currentKeyMapAction.getPress());
		} else
			mk = seq.addAction(KeyMapAction.class, seq.getLastInputCode());
		macros.addAction(mk);
	}

	@FXML
	void evtDelete() {
		macros.deleteSelected();
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
				macros.buildTree();
			} catch (IOException ioe) {
				LOG.log(java.lang.System.Logger.Level.ERROR, "Failed to export macros.", ioe);
			}
		}
	}

	@FXML
	void evtCommandBrowse() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle(bundle.getString("selectExecutable"));
		var path = commandLocation.textProperty().get();
		if (path == null || path.equals(""))
			path = System.getProperty("user.dir");
		JavaFX.selectFilesDir(fileChooser, path);
		File file = fileChooser.showOpenDialog((Stage) getScene().getWindow());
		if (file != null) {
			commandLocation.textProperty().set(file.getPath());
			var macro = (ExecuteMapAction) getSelectedMapAction();
			macro.setCommand(commandLocation.textProperty().get());
			saveMacroKey(macro);
		}
	}

	private void saveMacroSequence(MapSequence sequence) {
		try {
			sequence.validate();
			if (task != null) {
				task.cancel(false);
			}
			error((String)null);
			synchronized (sequencesToSave) {
				sequencesToSave.add(sequence);
			}
			task = executor.schedule(() -> {
				synchronized (macrosToSave) {
					try {
						for (MapSequence m : sequencesToSave) {
							m.commit();
						}
					} finally {
						sequencesToSave.clear();
					}
				}
			}, 1000, TimeUnit.MILLISECONDS);
		} catch (ValidationException ve) {
			synchronized (sequencesToSave) {
				sequencesToSave.remove(sequence);
			}
			error(ve.getMessage());
		}
	}

	private void saveMacroKey(MapAction mkey) {
		try {
			mkey.validate();
			if (task != null) {
				task.cancel(false);
			}
			error((String)null);
			synchronized (macrosToSave) {
				macrosToSave.add(mkey);
			}
			task = executor.schedule(() -> {
				synchronized (macrosToSave) {
					try {
						for (MapAction m : macrosToSave) {
							m.commit();
						}
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

	private void updateState() {
		var seq = getSelectedSequence();
		var macro = getSelectedMapAction();
		export.setVisible(!map.getSequences().isEmpty());
		sequenceEditor.setVisible(seq != null);
		addMapAction.setVisible(seq != null);
		delete.setVisible(seq != null);
		editor.visibleProperty().set(macro != null);
		recordMacro.setVisible(seq != null && macro == null);
		if (macro == null) {
			delete.textProperty().set(bundle.getString("deleteSequence"));
		} else {
			delete.textProperty().set(bundle.getString("delete"));
		}
	}

	private void updateAction(MapAction macro) {
		if (macro != null) {
			if (macro instanceof KeyMapAction) {
				KeyMapAction macroKey = (KeyMapAction) macro;
				keyCode.getSelectionModel().select(macroKey.getPress());
			} else if (macro instanceof ReleaseMapAction) {
				ReleaseMapAction macroKey = (ReleaseMapAction) macro;
				keyCode.getSelectionModel().select(macroKey.getRelease());
			} else if (macro instanceof SleepMapAction) {
				SleepMapAction macroKey = (SleepMapAction) macro;
				seconds.textProperty().set(String.valueOf(macroKey.getSeconds()));
			} else if (macro instanceof MapMapAction) {
				MapMapAction macroKey = (MapMapAction) macro;
				if (!targetMap.getItems().isEmpty()) {
					if (StringUtils.isBlank(macroKey.getMapName())
							|| !targetMap.getItems().contains(macroKey.getMapName())) {
						targetMap.getSelectionModel().select(0);
						macroKey.setValue(targetMap.getSelectionModel().getSelectedItem());
						saveMacroKey(macroKey);
					} else
						targetMap.getSelectionModel().select(macroKey.getMapName());
				}
			} else if (macro instanceof ShiftMapAction) {
				ShiftMapAction macroKey = (ShiftMapAction) macro;
				if (!targetMap.getItems().isEmpty()) {
					if (StringUtils.isBlank(macroKey.getMapName())
							|| !targetMap.getItems().contains(macroKey.getMapName())) {
						targetMap.getSelectionModel().select(0);
						macroKey.setValue(targetMap.getSelectionModel().getSelectedItem());
						saveMacroKey(macroKey);
					} else
						targetMap.getSelectionModel().select(macroKey.getMapName());
				}
			} else if (macro instanceof ProfileMapAction) {
				ProfileMapAction macroKey = (ProfileMapAction) macro;
				if (!targetProfile.getItems().isEmpty()) {
					if (StringUtils.isBlank(macroKey.getProfileName())
							|| !targetProfile.getItems().contains(macroKey.getProfileName())) {
						targetProfile.getSelectionModel().select(0);
						macroKey.setValue(targetProfile.getSelectionModel().getSelectedItem());
						saveMacroKey(macroKey);
					} else
						targetProfile.getSelectionModel().select(macroKey.getProfileName());
				}
			} else if (macro instanceof ExecuteMapAction) {
				ExecuteMapAction macroScript = (ExecuteMapAction) macro;
				commandLocation.textProperty().set(macroScript.getCommand());
				if (macroScript.getArgs() == null || macroScript.getArgs().isEmpty())
					commandArgs.textProperty().set("");
				else
					commandArgs.textProperty().set(String.join("\n", macroScript.getArgs()));
			} else
				throw new UnsupportedOperationException();
		}
	}

	private void updateForSequence(MapSequence seq) {
		macroKey.getSelectionModel().select(seq == null ? null : seq.getMacroKey());
	}

	interface MacrosView {
		void refresh();

		Node getNode();

		void deleteSelected();

		void addAction(MapAction mk);

		void addSequence(MapSequence seq);

		<A extends MapAction> void add(MapSequence sequemce, MapAction action, Class<A> clazz,
				Callable<Object> defaultValue) throws Exception;

		void buildTree();

//		ArrayList<MapAction> getSelectionModel();
		MultipleSelectionModel<MapSequence> getSequenceSelectionModel();

		MultipleSelectionModel<MapAction> getActionSelectionModel();
	}

	static class LayoutMacrosView extends StackPane implements MacrosView, Viewer {

		ProfileMap map;
		SimpleBooleanProperty selectable = new SimpleBooleanProperty(true);
		SimpleBooleanProperty readOnly = new SimpleBooleanProperty(true);
		LayoutEditor layoutEditor;
		ListMultipleSelectionModel<MapSequence> sequenceSelectionModel;
		ListMultipleSelectionModel<MapAction> actionSelectionModel;
		MapSequence lastSequence;

		LayoutMacrosView(ProfileMap map, DeviceView view, App context) {
			super();
			this.map = map;

			setPrefWidth(500);

			layoutEditor = new LayoutEditor(context);
			layoutEditor.getStyleClass().add("padded");
			layoutEditor.setShowElementGraphics(false);
			layoutEditor.setComponentType(ComponentType.AREA);
//			layoutEditor.setLabelFactory((el) -> {
//				if (el instanceof Area) {
//					Area area = (Area) el;
//					Region.Name regionName = area.getRegion();
//					return regions.get(regionName);
//				}
//				return null;
//			});
			layoutEditor.open(map.getProfile().getDevice(), view, this);
			getChildren().add(layoutEditor);

			sequenceSelectionModel = new ListMultipleSelectionModel<>(new ObservableListBase<>() {

				@Override
				public MapSequence get(int index) {
					try {
					List<IO> els = layoutEditor.getElements();
					return getSequenceForKey((Key) els.get(index));
					}
					catch(Exception e) {
						e.printStackTrace();
						return null;
					}
				
				}

				@Override
				public int size() {
					return layoutEditor.getElements().size();
				}
			});
			sequenceSelectionModel.setSelectionMode(SelectionMode.SINGLE);
			actionSelectionModel = new ListMultipleSelectionModel<>(new ObservableListBase<>() {

				@Override
				public MapAction get(int index) {
					// TODO
					return null;
				}

				@Override
				public int size() {
					// TODO
					return 0;
				}
			});
			actionSelectionModel.setSelectionMode(SelectionMode.SINGLE);
			layoutEditor.getElementSelectionModel().setSelectionMode(SelectionMode.SINGLE);

			layoutEditor.getElementSelectionModel().selectedIndexProperty().addListener((e, oldVal, newVal) -> {
				checkSelectionChange();
			});

		}

		protected MapSequence getSequenceForKey(Key key) {
			if (key == null)
				return null;
			EventCode code = key.getEventCode();
			if (code != null) {
				return map.getSequences().get(code);
			}
			return null;
		}

		protected void checkSelectionChange() {
			int idx = layoutEditor.getElementSelectionModel().getSelectedIndex();
			IO item = idx == -1 ? null : layoutEditor.getElements().get(idx);
			Key key = (Key) item;
			MapSequence seq = getSequenceForKey(key);
			if (seq == null && key != null && key.getEventCode() != null) {
				seq = map.addSequence(key.getEventCode(), false);
			}
			if (!Objects.equals(seq, lastSequence)) {
				lastSequence = seq;
				sequenceSelectionModel.select(lastSequence);
			}

//			TreeItem<Object> seqItem = getSelectedSequenceItem();
//			TreeItem<Object> actionItem = getSelectedActionItem();

//			if (!Objects.equals(sequence.getValue(), lastSequence)) {
//				lastSequence = (MapSequence) seqItem.getValue();
//				sequenceSelectionModel.select(lastSequence);
//			}
//			if (!Objects.equals(seqItem.getValue(), lastAction)) {
//				lastAction = (MapAction) actionItem.getValue();
//				actionSelectionModel.select(lastAction);
//			}
		}

		@Override
		public void addListener(ViewerListener listener) {
		}

		@Override
		public void removeListener(ViewerListener listener) {
		}

		@Override
		public void removeSelectedElements() {
		}

		@Override
		public List<ComponentType> getEnabledTypes() {
			return Arrays.asList(ComponentType.KEY);
		}

		@Override
		public SimpleBooleanProperty selectableElements() {
			return selectable;
		}

		@Override
		public SimpleBooleanProperty readOnly() {
			return readOnly;
		}

		@Override
		public void deleteSelected() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void addAction(MapAction mk) {
			layoutEditor.refresh();
		}

		@Override
		public void addSequence(MapSequence seq) {
			layoutEditor.refresh();
		}

		@Override
		public <A extends MapAction> void add(MapSequence sequemce, MapAction action, Class<A> clazz,
				Callable<Object> defaultValue) throws Exception {
			throw new UnsupportedOperationException();
		}

		@Override
		public void buildTree() {
		}

		@Override
		public MultipleSelectionModel<MapSequence> getSequenceSelectionModel() {
			return sequenceSelectionModel;
		}

		@Override
		public MultipleSelectionModel<MapAction> getActionSelectionModel() {
			return actionSelectionModel;
		}

		@Override
		public Node getNode() {
			return this;
		}

		@Override
		public void setEnabledTypes(List<ComponentType> enabledTypes) {
		}

		@Override
		public void refresh() {
		}
	}

	static class TreeMacrosView extends TreeView<Object> implements MacrosView {

		ProfileMap map;
		App context;
		private MultipleSelectionModel<MapSequence> sequenceSelectionModel;
		private MultipleSelectionModel<MapAction> actionSelectionModel;
		private MapSequence lastSequence;
		private MapAction lastAction;

		TreeMacrosView(ProfileMap map, App context) {
			this.map = map;
			this.context = context;

			setCellFactory((list) -> {
				return new MacroListCell();
			});
			TreeItem<Object> root = new TreeItem<>();
			rootProperty().set(root);
			setShowRoot(false);
			getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

			sequenceSelectionModel = new ListMultipleSelectionModel<>(new BasicList<>());
			sequenceSelectionModel.setSelectionMode(SelectionMode.SINGLE);
			actionSelectionModel = new ListMultipleSelectionModel<>(new BasicList<>());
			actionSelectionModel.setSelectionMode(SelectionMode.SINGLE);

			getSelectionModel().selectedItemProperty().addListener((a) -> {
				checkSelectionChange();
			});

		}

		protected void checkSelectionChange() {
			TreeItem<Object> seqItem = getSelectedSequenceItem();
			TreeItem<Object> actionItem = getSelectedActionItem();

			if (!Objects.equals(seqItem.getValue(), lastSequence)) {
				lastSequence = (MapSequence) seqItem.getValue();
				sequenceSelectionModel.select(lastSequence);
			}
			if (!Objects.equals(seqItem.getValue(), lastAction)) {
				lastAction = (MapAction) actionItem.getValue();
				actionSelectionModel.select(lastAction);
			}
		}

		protected TreeItem<Object> getSelectedSequenceItem() {
			TreeItem<Object> sel = getSelectionModel().isEmpty() ? null : getSelectionModel().getSelectedItem();
			if (sel != null && sel.getValue() instanceof MapSequence)
				return sel;
			else if (sel != null && sel.getParent().getValue() instanceof MapSequence)
				return sel.getParent();
			else
				return null;
		}

		protected TreeItem<Object> getSelectedActionItem() {
			TreeItem<Object> sel = getSelectionModel().isEmpty() ? null : getSelectionModel().getSelectedItem();
			if (sel != null && sel.getValue() instanceof MapAction)
				return sel;
			else
				return null;
		}

		public void buildTree() {
			getRoot().getChildren().clear();
			var root = rootProperty().get();
			for (Map.Entry<EventCode, MapSequence> en : map.getSequences().entrySet()) {
				MapSequence seq = en.getValue();
				TreeItem<Object> macroSequence = addSequenceNode(seq);
				if (root.getChildren().size() == 1) {
					macroSequence.setExpanded(true);
					getSelectionModel().select(macroSequence);
				}
			}
		}

		private TreeItem<Object> addSequenceNode(MapSequence seq) {
			TreeItem<Object> macroSequence = new TreeItem<>(seq);
			for (MapAction m : seq)
				macroSequence.getChildren().add(new TreeItem<>(m));
			getRoot().getChildren().add(macroSequence);
			return macroSequence;
		}

		@Override
		public <A extends MapAction> void add(MapSequence sequemce, MapAction action, Class<A> clazz,
				Callable<Object> defaultValue) throws Exception {

			TreeItem<Object> seqItem = getSelectionModel().getSelectedItem().getParent();
			var idx = sequemce.indexOf(action);
			MapAction murl = action.update(clazz, defaultValue.call());

			TreeItem<Object> newMacroItem = new TreeItem<>(murl);
			seqItem.getChildren().set(idx, newMacroItem);
			getSelectionModel().select(newMacroItem);
		}

		@Override
		public MultipleSelectionModel<MapSequence> getSequenceSelectionModel() {
			return sequenceSelectionModel;
		}

		@Override
		public MultipleSelectionModel<MapAction> getActionSelectionModel() {
			return actionSelectionModel;
		}

		@Override
		public void addSequence(MapSequence seq) {
			TreeItem<Object> treeItem = addSequenceNode(seq);
			treeItem.setExpanded(true);
			getSelectionModel().select(treeItem.getChildren().get(0));
			requestFocus();
		}

		@Override
		public void addAction(MapAction mk) {
			TreeItem<Object> t = new TreeItem<>(mk);
			TreeItem<Object> seqItem = getSelectedSequenceItem();
			seqItem.setExpanded(true);
			seqItem.getChildren().add(t);
			getSelectionModel().select(t);
			requestFocus();

		}

		@Override
		public void deleteSelected() {

			TreeItem<Object> selectedItem = getSelectionModel().getSelectedItem();
			MapAction m = getActionSelectionModel().getSelectedItem();
			var seq = getSequenceSelectionModel().getSelectedItem();
			if (m == null) {
				Confirm confirm = context.push(Confirm.class, Direction.FADE);
				confirm.confirm(bundle, "removeSequence", () -> {
					getSelectionModel().clearSelection();
					seq.remove();
					selectedItem.getParent().getChildren().remove(selectedItem);
					// TODO
//					updateState();
				}, null, seq.getMacroKey().name());
			} else {
				m.remove();
				selectedItem.getParent().getChildren().remove(selectedItem);
				// TODO
//				updateState();
			}

		}

		@Override
		public Node getNode() {
			return this;
		}

	}
}
