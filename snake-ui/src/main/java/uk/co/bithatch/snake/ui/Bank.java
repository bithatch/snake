package uk.co.bithatch.snake.ui;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.UnaryOperator;
import java.util.prefs.Preferences;

import org.controlsfx.control.SearchableComboBox;
import org.controlsfx.control.ToggleSwitch;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import com.sshtools.icongenerator.IconBuilder.TextContent;
import com.sshtools.jfreedesktop.javafx.SVGIcon;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MultipleSelectionModel;
import javafx.scene.control.RadioButton;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.scene.control.ToggleGroup;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.converter.FloatStringConverter;
import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.linuxio.EventCode.Type;
import uk.co.bithatch.macrolib.Action;
import uk.co.bithatch.macrolib.ActionMacro;
import uk.co.bithatch.macrolib.Application;
import uk.co.bithatch.macrolib.CommandMacro;
import uk.co.bithatch.macrolib.KeySequence;
import uk.co.bithatch.macrolib.Macro;
import uk.co.bithatch.macrolib.MacroBank;
import uk.co.bithatch.macrolib.MacroProfile;
import uk.co.bithatch.macrolib.NoopMacro;
import uk.co.bithatch.macrolib.RepeatMode;
import uk.co.bithatch.macrolib.ScriptMacro;
import uk.co.bithatch.macrolib.SimpleMacro;
import uk.co.bithatch.macrolib.TargetType;
import uk.co.bithatch.macrolib.UInputMacro;
import uk.co.bithatch.macrolib.View;
import uk.co.bithatch.macrolib.Window;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.ValidationException;
import uk.co.bithatch.snake.lib.layouts.ComponentType;
import uk.co.bithatch.snake.lib.layouts.DeviceLayout;
import uk.co.bithatch.snake.lib.layouts.DeviceView;
import uk.co.bithatch.snake.lib.layouts.IO;
import uk.co.bithatch.snake.lib.layouts.Key;
import uk.co.bithatch.snake.ui.designer.LayoutEditor;
import uk.co.bithatch.snake.ui.designer.Viewer;
import uk.co.bithatch.snake.ui.util.BasicList;
import uk.co.bithatch.snake.ui.util.Strings;
import uk.co.bithatch.snake.ui.widgets.MacroBankLEDHelper;
import uk.co.bithatch.snake.widgets.Direction;
import uk.co.bithatch.snake.widgets.GeneratedIcon;
import uk.co.bithatch.snake.widgets.JavaFX;
import uk.co.bithatch.snake.widgets.ProfileLEDs;

public class Bank extends AbstractDetailsController {

	public static String textForMapSequence(KeySequence seq) {
		return textForInputEvent(seq.get(0));
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

	public static GeneratedIcon iconForMapSequence(KeySequence seq) {
		GeneratedIcon icon = new GeneratedIcon();
		icon.getStyleClass().add("mapSequence");
		icon.setPrefHeight(32);
		icon.setPrefWidth(32);
		String keyName = String.valueOf(seq.get(0));
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

	private static class MacroListCell extends ListCell<KeySequence> {
		@Override
		public void updateItem(KeySequence item, boolean empty) {
			super.updateItem(item, empty);
			if (empty) {
				setGraphic(null);
				setText(null);
			} else {
				setGraphic(iconForMapSequence(item));
				setText(textForMapSequence(item));
			}
		}
	}

	private static class ActionListCell extends ListCell<Action> {
		@Override
		public void updateItem(Action item, boolean empty) {
			super.updateItem(item, empty);
			setGraphic(null);
			if (empty) {
				setText(null);
			} else {
				setText(item.getId());
			}
		}
	}

	final static ResourceBundle bundle = ResourceBundle.getBundle(Bank.class.getName());

	final static Preferences PREFS = Preferences.userNodeForPackage(Bank.class);

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
	private SearchableComboBox<EventCode> macroKey;
	@FXML
	private ComboBox<TargetType> targetType;
	@FXML
	private ToggleGroup macroType;
	@FXML
	private TextField repeatDelay;
	@FXML
	private TextArea commandArgs;
	@FXML
	private TextArea script;
	@FXML
	private Button commandBrowse;
	@FXML
	private CheckBox passthrough;
	@FXML
	private Spinner<Integer> value;
	@FXML
	private TextField commandLocation;
	@FXML
	private TextField simpleMacroText;
	@FXML
	private VBox commandSection;
	@FXML
	private VBox scriptSection;
	@FXML
	private VBox repeatSection;
	@FXML
	private VBox unmappedSection;
	@FXML
	private VBox actionSection;
	@FXML
	private VBox newMacroSection;
	@FXML
	private VBox sequenceEditor;
	@FXML
	private VBox simpleMacroSection;
	@FXML
	private SearchableComboBox<EventCode> keyCode;
	@FXML
	private RadioButton uinputMacro;
	@FXML
	private RadioButton commandMacro;
	@FXML
	private RadioButton noopMacro;
	@FXML
	private RadioButton scriptMacro;
	@FXML
	private RadioButton simpleMacro;
	@FXML
	private RadioButton actionMacro;
	@FXML
	private Hyperlink recordMacro;
	@FXML
	private ComboBox<Action> targetAction;
	@FXML
	private BorderPane macrosContainer;
	@FXML
	private BorderPane viewContainer;
	@FXML
	private ComboBox<RepeatMode> repeatMode;
	@FXML
	private Label title;
	@FXML
	private ToggleSwitch defaultBank;
	@FXML
	private ToggleSwitch defaultProfile;
	@FXML
	private Label defaultBankOnText;
	@FXML
	private Label defaultBankOffText;
	@FXML
	private Label defaultProfileOnText;
	@FXML
	private Label defaultProfileOffText;
	@FXML
	private Label profileLEDLabel;
	@FXML
	private TextField bankName;
	@FXML
	private ProfileLEDs profileLED;
	@FXML
	private TextField profileName;
	@FXML
	private ComboBox<uk.co.bithatch.macrolib.Window> window;
	@FXML
	private ComboBox<Application> application;
	@FXML
	private ToggleGroup profileLEDMode;
	@FXML
	private RadioButton automatic;
	@FXML
	private RadioButton manual;
	@FXML
	private Hyperlink addApplication;
	@FXML
	private Hyperlink addWindow;
	@FXML
	private Hyperlink removeApplication;
	@FXML
	private Hyperlink removeWindow;
	@FXML
	private ListView<Application> includeApplication;
	@FXML
	private ListView<Application> excludeApplication;
	@FXML
	private ListView<uk.co.bithatch.macrolib.Window> includeWindow;
	@FXML
	private ListView<uk.co.bithatch.macrolib.Window> excludeWindow;
	@FXML
	private TabPane applicationsTabs;
	@FXML
	private TabPane windowsTabs;
	@FXML
	private TextField windowRegExp;
	@FXML
	private TextField applicationRegExp;

	// TODO needed?
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

	private MacroBank bank;
	private ScheduledFuture<?> task;
	private boolean adjusting = false;
	private MacrosView macros;
	private LayoutEditor layoutEditor;
	private Macro macro;

	private MacroBankLEDHelper profileLEDHelper;

	protected ContextMenu createContextMenu() {
		ContextMenu menu = new ContextMenu();
		MenuItem remove = new MenuItem(bundle.getString("contextMenuRemove"));
		remove.onActionProperty().set((e) -> {
			evtDelete();
		});
		menu.getItems().add(remove);
		if (macros != null) {
			MenuItem add = new MenuItem(bundle.getString("contextMenuAdd"));
			add.onActionProperty().set((e) -> {
				add(macros.getSequenceSelectionModel().getSelectedItem());
			});
			menu.getItems().add(add);
			menu.setOnShowing((e) -> {
				remove.disableProperty().set(!bank.contains(macros.getSequenceSelectionModel().getSelectedItem()));
				add.disableProperty().set(bank.contains(macros.getSequenceSelectionModel().getSelectedItem()));
			});
		} else {
			menu.setOnShowing((e) -> {
				remove.disableProperty().set(!bank.contains(macros.getSequenceSelectionModel().getSelectedItem()));
			});
		}
		return menu;
	}

	protected void error(String key) {
		if (key == null)
			clearNotifications(false);
		else {
			String txt = bundle.getString(key);
			notifyMessage(MessageType.DANGER, txt == null ? "<missing key " + key + ">" : txt);
		}
	}

	@Override
	protected void onDeviceCleanUp() {
		if (LOG.isLoggable(Level.DEBUG))
			LOG.log(Level.DEBUG, "Stopping macros scheduler.");
		if (profileLEDHelper != null) {
			try {
				profileLEDHelper.close();
			} catch (IOException e1) {
			}
		}
		executor.shutdown();
		if (layoutEditor != null) {
			try {
				layoutEditor.close();
			} catch (Exception e) {
			}
		}
	}

	public void setBank(MacroBank bank) throws Exception {
		/* Set convenience instance variables */
		this.bank = bank;

		/* Get current profile, bank etc */
		var macroManager = context.getMacroManager();
		var macroDevice = bank.getProfile().getDevice();
		var device = macroManager.getNativeDevice(macroDevice);

		/* Simple bindings */
		JavaFX.bindManagedToVisible(commandSection, keySection, repeatSection, editor, actionSection, actionMacro,
				simpleMacroSection, scriptSection, recordMacro, newMacroSection, unmappedSection, add,
				defaultProfileOnText, defaultProfileOffText, defaultBankOnText, defaultBankOffText, profileLEDLabel,
				profileLED, automatic, manual);

		profileLED.disableProperty().bind(automatic.selectedProperty());
		profileLEDLabel.visibleProperty().bind(profileLED.visibleProperty());
		automatic.visibleProperty().bind(profileLED.visibleProperty());
		manual.visibleProperty().bind(profileLED.visibleProperty());
		defaultProfile.setDisable(bank.getProfile().getSystem().getNumberOfProfiles(bank.getProfile().getDevice()) < 2);
		defaultBank.setDisable(bank.getProfile().getBanks().size() < 2);
		defaultProfileOffText.visibleProperty().bind(defaultProfile.selectedProperty());
		defaultProfileOnText.visibleProperty().bind(Bindings.not(defaultProfile.selectedProperty()));
		defaultBank.selectedProperty().addListener((e, o, n) -> {
			try {
				if (n)
					bank.getProfile().makeActive();
				else {
					for (Iterator<MacroProfile> profileIt = bank.getProfile().getSystem()
							.profiles(bank.getProfile().getDevice()); profileIt.hasNext();) {
						MacroProfile p = profileIt.next();
						if (p != bank.getProfile()) {
							p.makeActive();
							break;
						}
					}
					if (bank.getProfile().isActive())
						throw new Exception("No other profilea.");
				}
			} catch (Exception ex) {
				error(ex);
			}
		});
		defaultBankOffText.visibleProperty().bind(defaultBank.selectedProperty());
		defaultBankOnText.visibleProperty().bind(Bindings.not(defaultBank.selectedProperty()));
		defaultBank.selectedProperty().addListener((e, o, n) -> {
			try {
				if (n)
					bank.makeActive();
				else {
					for (MacroBank b : bank.getProfile().getBanks()) {
						if (b != bank) {
							b.makeActive();
							break;
						}
					}
					if (bank.isActive())
						throw new Exception("No other banks.");
				}
			} catch (Exception ex) {
				error(ex);
			}
		});

		/* Title */
		setTitle();

		/* Windows and applications */
		Callback<ListView<uk.co.bithatch.macrolib.Window>, ListCell<uk.co.bithatch.macrolib.Window>> cellFactory = createWindowCellFactory(
				true);
		window.setButtonCell(cellFactory.call(null));
		window.setCellFactory(cellFactory);
		includeWindow.setCellFactory(createWindowCellFactory(false));
		excludeWindow.setCellFactory(createWindowCellFactory(false));
		window.getItems().addAll(macroManager.getMacroSystem().getMonitor().getWindows());
		window.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> {
			windowRegExp.textProperty().set("");
		});
		Callback<ListView<Application>, ListCell<Application>> applicationCellFactory = createApplicationCellFactory(
				true);
		application.setCellFactory(applicationCellFactory);
		application.setButtonCell(applicationCellFactory.call(null));
		application.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> {
			applicationRegExp.textProperty().set("");
		});
		applicationRegExp.textProperty().addListener((c, o, n) -> {
			if(!n.equals(""))
				application.getSelectionModel().clearSelection();
		});
		windowRegExp.textProperty().addListener((c, o, n) -> {
			if(!n.equals(""))
				window.getSelectionModel().clearSelection();
		});
		includeApplication.setCellFactory(createApplicationCellFactory(false));
		excludeApplication.setCellFactory(createApplicationCellFactory(false));
		application.getItems().addAll(macroManager.getMacroSystem().getMonitor().getApplications());
		includeApplication.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> updateAvailability());
		excludeApplication.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> updateAvailability());
		includeWindow.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> updateAvailability());
		excludeWindow.getSelectionModel().selectedItemProperty().addListener((c, o, n) -> updateAvailability());
		windowsTabs.getSelectionModel().selectedIndexProperty().addListener((c, o, n) -> updateAvailability());
		applicationsTabs.getSelectionModel().selectedIndexProperty().addListener((c, o, n) -> updateAvailability());

		/*
		 * If the device has a layout with some Key's, then replace the tree view with
		 * the layout view
		 */
		if (context.getLayouts().hasLayout(device)) {
			DeviceLayout layout = context.getLayouts().getLayout(device);
			DeviceView view = layout.getViewThatHas(ComponentType.KEY);
			if (view != null) {
				macros = new LayoutMacrosView(bank, view, context);
			}
		}
		if (macros == null) {
			macros = new ListMacrosView(bank, context);

			/*
			 * Also move the macro editor container into the center of the layout so the
			 * components stretch as expected. When in layout mode, we want the layout to
			 * take all the space, and the editor to have a slimmer column of space on the
			 * right. When in standard mode, we want the editor to take up the most space,
			 * and the list of macros to be a slimmer column down the left.
			 */
			Node center = viewContainer.getCenter();
			Node right = viewContainer.getRight();
			viewContainer.getChildren().remove(center);
			viewContainer.getChildren().remove(right);

			viewContainer.setLeft(center);
			viewContainer.setCenter(right);
			((Region) right).setPrefWidth(VBox.USE_COMPUTED_SIZE);
		} else
			add.setVisible(false);
		macros.setContextMenu(createContextMenu());
		macrosContainer.setCenter(macros.getNode());

		/* The components whose visibility we can easily bind to other simple state */
		commandSection.visibleProperty().bind(commandMacro.selectedProperty());
		scriptSection.visibleProperty().bind(scriptMacro.selectedProperty());
		actionSection.visibleProperty().bind(actionMacro.selectedProperty());
		keySection.visibleProperty().bind(uinputMacro.selectedProperty());
		simpleMacroSection.visibleProperty().bind(simpleMacro.selectedProperty());
		repeatSection.visibleProperty().bind(Bindings.not(noopMacro.selectedProperty()));
		value.disableProperty().bind(passthrough.selectedProperty());
		targetType.disableProperty().bind(passthrough.selectedProperty());

		/* Set initial state of various components */
		uinputMacro.selectedProperty().set(true);
		macroKey.itemsProperty().get()
				.addAll(EventCode.filteredForType(macroDevice.getSupportedInputEvents(), Type.EV_KEY));
		targetAction.getItems().addAll(macroManager.getMacroSystem().getActions().values());
		repeatMode.getItems().addAll(Arrays.asList(RepeatMode.values()));
		targetType.getItems().addAll(Arrays.asList(TargetType.uinputTypes()));
		keyCode.itemsProperty().get().addAll(EventCode.values());
		value.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, Short.MAX_VALUE, 0, 1));

		/* Configure components rendering / input */
		UnaryOperator<Change> floatFilter = change -> {
			String newText = change.getControlNewText();
			if (newText.matches("[-+]?([0-9]*\\.[0-9]+|[0-9]+)")) {
				return change;
			}
			return null;
		};
		repeatDelay.setTextFormatter(new TextFormatter<Float>(new FloatStringConverter(), 0f, floatFilter));
		targetAction.setCellFactory((list) -> {
			return new ActionListCell();
		});
		bankName.setText(bank.getName());
		profileName.setText(bank.getProfile().getName());

		/*
		 * Listen to various state changes and adjust other state and selection
		 * accordingly
		 */
		bankName.textProperty().addListener((e, o, n) -> {
			bank.setName(n);
			saveBank();
			setTitle();
		});
		profileName.textProperty().addListener((e, o, n) -> {
			bank.getProfile().setName(n);
			saveProfile();
			setTitle();
		});
		keyCode.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting && n != null) {
				((UInputMacro) macro).setCode(n);
				save(macro);
				macros.refresh();
			}
		});
		commandLocation.textProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				((CommandMacro) macro).setCommand(commandLocation.textProperty().get());
				save(macro);
			}
		});
		simpleMacroText.textProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				((SimpleMacro) macro).setMacro(simpleMacroText.textProperty().get());
				save(macro);
			}
		});
		repeatDelay.textProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				try {
					macro.setRepeatDelay(Float.parseFloat(repeatDelay.textProperty().get()));
					save(macro);
				} catch (NumberFormatException nfe) {
					error("invalidPause");
				}
			}
		});
		commandArgs.textProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				((CommandMacro) macro).setArguments(parseQuotedString(commandArgs.textProperty().get()));
				save(macro);
			}
		});
		script.textProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				((ScriptMacro) macro).setScript(script.textProperty().get());
				save(macro);
			}
		});
		repeatMode.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				if (macro != null) {
					macro.setRepeatMode(n);
					save(macro);
				}
			}
		});
		targetType.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				if (macro != null) {
					macro.setType(n);
					save(macro);
				}
			}
		});
		passthrough.selectedProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				if (macro != null) {
					if (n)
						((UInputMacro) macro).setType(TargetType.forEvent(macro.getActivatedBy().get(0)));
					((UInputMacro) macro).setPassthrough(n);
					save(macro);
				}
			}
		});
		value.valueProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				if (macro != null) {
					((UInputMacro) macro).setValue(n);
					save(macro);
				}
			}
		});
		targetAction.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				if (macro != null) {
					((ActionMacro) macro).setAction(n == null ? null : n.getId());
					save(macro);
				}
			}
		});
		macroKey.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				if (macro != null && n != null) {
					macro.getActivatedBy().clear();
					macro.getActivatedBy().add(macroKey.getSelectionModel().getSelectedItem());
					save(macro);
					macros.refresh();
				}
			}
		});
		macros.getSequenceSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (!adjusting) {
				adjusting = true;
				try {
					KeySequence key = macros.getSequenceSelectionModel().getSelectedItem();
					macro = key == null ? null : bank.getMacro(key);
//					if (macro == null && key != null) {
//						macro = createDefaultMacro(key);
//					}
					updateForMacro(macro, key != null && key.isEmpty());
				} finally {
					adjusting = false;
				}
			}
		});

		/* Watch for the type of macro changing and adjust accordingly */
		uinputMacro.selectedProperty().addListener((e, o, n) -> {
			if (!adjusting && n) {
				bank.remove(macro);
				macro = new UInputMacro(macro);
				if (keyCode.getSelectionModel().getSelectedItem() == null)
					keyCode.getSelectionModel().select(macroKey.getSelectionModel().getSelectedItem());
				bank.add(macro);
				updateForMacro(macro, false);
				save(macro);
			}
		});
		noopMacro.selectedProperty().addListener((e, o, n) -> {
			if (!adjusting && n) {
				bank.remove(macro);
				macro = new NoopMacro(macro);
				bank.add(macro);
				updateForMacro(macro, false);
				save(macro);
			}
		});
		scriptMacro.selectedProperty().addListener((e, o, n) -> {
			if (!adjusting && n) {
				bank.remove(macro);
				macro = new ScriptMacro(macro);
				bank.add(macro);
				updateForMacro(macro, false);
				save(macro);
			}
		});
		commandMacro.selectedProperty().addListener((e, o, n) -> {
			if (!adjusting && n) {
				bank.remove(macro);
				macro = new CommandMacro(macro);
				bank.add(macro);
				updateForMacro(macro, false);
				save(macro);
			}
		});
		actionMacro.selectedProperty().addListener((e, o, n) -> {
			if (!adjusting && n) {
				bank.remove(macro);
				macro = new ActionMacro(macro);
				((ActionMacro) macro)
						.setAction(targetAction.getItems().isEmpty() ? null : targetAction.getItems().get(0).getId());
				bank.add(macro);
				updateForMacro(macro, false);
				save(macro);
			}
		});

		if (getDevice().getCapabilities().contains(Capability.PROFILE_LEDS)) {
			profileLEDHelper = new MacroBankLEDHelper(context.getMacroManager().getMacroSystem(), bank, profileLED);
			automatic.setSelected(profileLEDHelper.isAutomatic());
			manual.setSelected(!profileLEDHelper.isAutomatic());
		} else {
			automatic.setSelected(true);
			profileLED.setVisible(false);
		}

		macros.buildTree();
		adjusting = true;
		try {
			updateForMacro(macro, false);
		} finally {
			adjusting = false;
		}
		updateAvailability();
	}

	protected Callback<ListView<Application>, ListCell<Application>> createApplicationCellFactory(
			boolean showChooseText) {
		return (l) -> {
			return new ListCell<Application>() {
				@Override
				protected void updateItem(Application item, boolean empty) {
					super.updateItem(item, empty);
					if (item == null || empty) {
						setGraphic(null);
						if (showChooseText)
							setText(bundle.getString("chooseApplication"));
						else
							setText("");
					} else {
						String icon = item.getIcon();
						if (icon != null && icon.length() > 0) {
							setGraphic(getViewIcon(item));
						} else {
							setGraphic(new FontIcon(FontAwesome.BARS));
						}
						setText(item.getName());
					}
				}
			};
		};
	}

	protected Callback<ListView<Window>, ListCell<Window>> createWindowCellFactory(boolean showChooseText) {
		return (l) -> {
			return new ListCell<uk.co.bithatch.macrolib.Window>() {
				@Override
				protected void updateItem(uk.co.bithatch.macrolib.Window item, boolean empty) {
					super.updateItem(item, empty);
					if (item == null || empty) {
						setGraphic(null);
						if (showChooseText)
							setText(bundle.getString("chooseWindow"));
						else
							setText("");
					} else {
						String icon = item.getIcon();
						if (icon != null && icon.length() > 0) {
							setGraphic(getViewIcon(item));
						} else {
							setGraphic(new FontIcon(FontAwesome.WINDOW_MAXIMIZE));
						}
						setText(item.getName());
					}
				}
			};
		};
	}

	protected Node getViewIcon(View view) {
		try {
			String icon = view.getIcon();
			String path;
			if (icon.contains("/")) {
				path = Paths.get(icon).toUri().toURL().toExternalForm();
			} else {
				path = context.getMacroManager().getMacroSystem().getIconService().findIcon(icon, 24).toUri().toURL()
						.toExternalForm();
			}
			if (path != null) {
				if (path.endsWith(".svg")) {
					try (InputStream in = new URL(path).openStream()) {
						return new SVGIcon(path, in, 22, 22);
					}
				} else {
					ImageView iv = new ImageView(path);
					iv.setFitHeight(22);
					iv.setFitWidth(22);
					return iv;
				}
			}
		} catch (IOException e) {
		}
		return new Label("?");
	}

	protected void setTitle() {
		title.textProperty().set(MessageFormat.format(bundle.getString("title"), bank.getBank(), bank.getName(),
				bank.getProfile().getName()));
	}

	protected UInputMacro createDefaultMacro(KeySequence key) {
		UInputMacro ui = new UInputMacro(key, key.get(0));
		return ui;
	}

	protected void updateForMacro(Macro macro, boolean unmapped) {

		macroKey.getSelectionModel().select(macro == null ? null : macro.getActivatedBy().get(0));
		export.setVisible(true);

		if (unmapped) {
			unmappedSection.setVisible(true);
			newMacroSection.setVisible(false);
			sequenceEditor.setVisible(false);
			delete.setVisible(false);
			editor.visibleProperty().set(false);
			recordMacro.setVisible(false);
		} else if (macro == null) {
			unmappedSection.setVisible(false);
			newMacroSection.setVisible(true);
			sequenceEditor.setVisible(false);
			delete.setVisible(false);
			editor.visibleProperty().set(false);
			recordMacro.setVisible(false);
		} else {
			unmappedSection.setVisible(false);
			sequenceEditor.setVisible(true);
			delete.setVisible(true);
			editor.visibleProperty().set(true);
			recordMacro.setVisible(true);
			newMacroSection.setVisible(false);
			repeatDelay.textProperty().set(String.valueOf(macro.getRepeatDelay()));
			repeatMode.getSelectionModel().select(macro.getRepeatMode());

			if (macro instanceof UInputMacro) {
				uinputMacro.selectedProperty().set(true);
				var uinputMacroObj = (UInputMacro) macro;
				keyCode.getSelectionModel().select(uinputMacroObj.getCode());
				if (uinputMacroObj.isPassthrough())
					targetType.getSelectionModel().select(TargetType.forEvent(macro.getActivatedBy().get(0)));
				else
					targetType.getSelectionModel().select(uinputMacroObj.getType());
				passthrough.setSelected(uinputMacroObj.isPassthrough());
				value.getValueFactory().setValue(uinputMacroObj.getValue());
			} else if (macro instanceof NoopMacro)
				noopMacro.selectedProperty().set(true);
			else if (macro instanceof ScriptMacro) {
				scriptMacro.selectedProperty().set(true);
				var scriptMacroObj = (ScriptMacro) macro;
				script.textProperty()
						.set(scriptMacroObj.getScript() == null ? "" : String.join("\n", scriptMacroObj.getScript()));
			} else if (macro instanceof CommandMacro) {
				commandMacro.selectedProperty().set(true);

				var commandMacroObj = (CommandMacro) macro;
				commandLocation.textProperty().set(commandMacroObj.getCommand());
				if (commandMacroObj.getArguments() == null || commandMacroObj.getArguments().length == 0)
					commandArgs.textProperty().set("");
				else
					commandArgs.textProperty().set(String.join("\n", commandMacroObj.getArguments()));
			} else if (macro instanceof SimpleMacro) {
				simpleMacro.selectedProperty().set(true);
				var simpleMacroObj = (SimpleMacro) macro;
				simpleMacroText.textProperty().set(String.valueOf(simpleMacroObj.getMacro()));
			} else if (macro instanceof ActionMacro) {
				actionMacro.selectedProperty().set(true);
				var actionMacroObj = (ActionMacro) macro;
				targetAction.getSelectionModel().select(actionMacroObj.getAction() == null ? null
						: context.getMacroManager().getMacroSystem().getActions().get(actionMacroObj.getAction()));
			} else
				throw new UnsupportedOperationException("Unknown action type.");
		}

	}

	@FXML
	void evtAddWindow() {
		uk.co.bithatch.macrolib.Window sel = window.getSelectionModel().getSelectedItem();
		if (sel != null) {
			if (windowsTabs.getSelectionModel().getSelectedIndex() == 0) {
				includeWindow.getItems().add(sel);
			} else {
				excludeWindow.getItems().add(sel);
			}
		}
	}

	@FXML
	void evtAddApplication() {
		Application sel = application.getSelectionModel().getSelectedItem();
		if (sel != null) {
			if (applicationsTabs.getSelectionModel().getSelectedIndex() == 0) {
				includeApplication.getItems().add(sel);
			} else {
				excludeApplication.getItems().add(sel);
			}
		}
	}

	@FXML
	void evtRemoveWindow() {
		if (windowsTabs.getSelectionModel().getSelectedIndex() == 0) {
			includeWindow.getItems().remove(includeWindow.getSelectionModel().getSelectedItem());
		} else {
			excludeWindow.getItems().remove(excludeWindow.getSelectionModel().getSelectedItem());
		}
	}

	@FXML
	void evtRemoveApplication() {
		if (applicationsTabs.getSelectionModel().getSelectedIndex() == 0) {
			includeApplication.getItems().remove(includeApplication.getSelectionModel().getSelectedItem());
		} else {
			excludeApplication.getItems().remove(excludeApplication.getSelectionModel().getSelectedItem());
		}
	}

	@FXML
	void evtAdd() {
		add(bank.getNextFreeActivationSequence());
	}

	protected void add(KeySequence seq) {
		try {
			adjusting = true;
			try {
				macro = createDefaultMacro(seq);
				bank.add(macro);
				macros.addSequence(macro);
				updateForMacro(macro, false);
				return;
			} finally {
				adjusting = false;
			}
		} catch (IllegalStateException e) {
			error("noKeysLeft");
		}
	}

	@FXML
	void evtRecordMacro() {
		context.push(Record.class, Direction.FROM_LEFT);
	}

	@FXML
	void evtManual() {
		profileLEDHelper.setAutomatic(false);
	}

	@FXML
	void evtAutomatic() {
		profileLEDHelper.setAutomatic(true);
	}

	@FXML
	void evtDelete() {
		Confirm confirm = context.push(Confirm.class, Direction.FADE);
		confirm.confirm(bundle, "removeSequence", () -> {
			macro.remove();
			try {
				macro.commit();
				macros.deleteSelected();
				updateForMacro(null, false);
			} catch (Exception e) {
				error(e);
			}
		}, null, macro.getDisplayName());
	}

	@FXML
	void evtExport() {
		Export confirm = context.push(Export.class, Direction.FADE);
//		confirm.export(addOn, bundle, "exportMacroProfile", effect.getName());

//		FileChooser fileChooser = new FileChooser();
//		fileChooser.setTitle(bundle.getString("selectExportFile"));
//		var path = PREFS.get("lastExportLocation", System.getProperty("user.dir") + File.separator + "macros.json");
//		fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("macroFileExtension"), "*.json"));
//		fileChooser.getExtensionFilters().add(new ExtensionFilter(bundle.getString("allFiles"), "*.*"));
//		JavaFX.selectFilesDir(fileChooser, path);
//		File file = fileChooser.showSaveDialog((Stage) getScene().getWindow());
//		if (file != null) {
//			PREFS.put("lastExportLocation", file.getAbsolutePath());
//			try (PrintWriter pw = new PrintWriter(file)) {
//				pw.println(getDevice().exportMacros());
//			} catch (IOException ioe) {
//				LOG.log(java.lang.System.Logger.Level.ERROR, "Failed to export macros.", ioe);
//			}
//		}
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
			((CommandMacro) macro).setCommand(commandLocation.textProperty().get());
			save(macro);
		}
	}

	private void save(Macro mkey) {
		try {
			error((String) null);
			context.getMacroManager().validate(mkey);
			if (task != null) {
				task.cancel(false);
			}
			task = executor.schedule(() -> {
				if (!bank.getMacros().contains(mkey)) {
					bank.add(mkey);
				}
				try {
					bank.commit();
				} catch (Exception e) {
					error(e);
				}
			}, 1000, TimeUnit.MILLISECONDS);
		} catch (ValidationException ve) {
			error(ve.getMessage());
		}
	}

	private void saveProfile() {
		error((String) null);
		if (task != null) {
			task.cancel(false);
		}
		task = executor.schedule(() -> {
			try {
				bank.getProfile().commit();
			} catch (Exception e) {
				error(e);
			}
		}, 1000, TimeUnit.MILLISECONDS);
	}

	private void saveBank() {
		error((String) null);
		if (task != null) {
			task.cancel(false);
		}
		task = executor.schedule(() -> {
			try {
				bank.commit();
			} catch (Exception e) {
				error(e);
			}
		}, 1000, TimeUnit.MILLISECONDS);
	}

	Macro getMacroForKey(EventCode eventCode) {
		return bank.getMacro(new KeySequence(eventCode));
	}

	interface MacrosView {
		void setContextMenu(ContextMenu contextMenu);

		void refresh();

		Node getNode();

		void deleteSelected();

		void addSequence(Macro seq);

		void buildTree();

		MultipleSelectionModel<KeySequence> getSequenceSelectionModel();
	}

	private void updateAvailability() {
		Application asel;
		if (applicationsTabs.getSelectionModel().getSelectedIndex() == 0) {
			asel = includeApplication.getSelectionModel().getSelectedItem();
		} else {
			asel = excludeApplication.getSelectionModel().getSelectedItem();
		}
		uk.co.bithatch.macrolib.Window wsel;
		if (windowsTabs.getSelectionModel().getSelectedIndex() == 0) {
			wsel = includeWindow.getSelectionModel().getSelectedItem();
		} else {
			wsel = excludeWindow.getSelectionModel().getSelectedItem();
		}
		removeApplication.setDisable(asel == null);
		removeWindow.setDisable(wsel == null);
	}

	class LayoutMacrosView extends StackPane implements MacrosView, Viewer {

		MacroBank bank;
		SimpleBooleanProperty selectable = new SimpleBooleanProperty(true);
		SimpleBooleanProperty readOnly = new SimpleBooleanProperty(true);
		LayoutEditor layoutEditor;
		ListMultipleSelectionModel<KeySequence> sequenceSelectionModel;
		KeySequence lastSequence;

		LayoutMacrosView(MacroBank map, DeviceView view, App context) {
			super();
			this.bank = map;

			layoutEditor = new LayoutEditor(context);
			layoutEditor.getStyleClass().add("padded");
			layoutEditor.setShowElementGraphics(false);
			layoutEditor.setComponentType(ComponentType.AREA);
			layoutEditor.open(context.getMacroManager().getNativeDevice(map.getProfile().getDevice()), view, this);
			getChildren().add(layoutEditor);
			sequenceSelectionModel = new ListMultipleSelectionModel<>(new BasicList<>());

			sequenceSelectionModel.setSelectionMode(SelectionMode.SINGLE);
			layoutEditor.getElementSelectionModel().setSelectionMode(SelectionMode.SINGLE);
			layoutEditor.getElementSelectionModel().selectedIndexProperty().addListener((e, oldVal, newVal) -> {
				checkSelectionChange();
			});

		}

		@Override
		public void setContextMenu(ContextMenu contextMenu) {
			layoutEditor.setMenuCallback((tool, evt) -> {
				contextMenu.show(tool, Side.BOTTOM, 0, 0);
			});
		}

		protected void checkSelectionChange() {
			int idx = layoutEditor.getElementSelectionModel().getSelectedIndex();
			IO item = idx == -1 ? null : layoutEditor.getElements().get(idx);
			Key key = (Key) item;
			KeySequence seq = null;
			if (key != null) {
				if (key.getEventCode() == null) {
					/*
					 * The key exists, but is not mapped to anything (e.g. an un-mappable DPI switch
					 * button)
					 */
					seq = new KeySequence();
				} else
					/* An actual sequence */
					seq = new KeySequence(key.getEventCode());
			}
			if (!Objects.equals(seq, lastSequence)) {
				lastSequence = seq;
				if (lastSequence == null)
					sequenceSelectionModel.clearSelection();
				else
					sequenceSelectionModel.select(lastSequence);
			}
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
			lastSequence = null;
			layoutEditor.getElementSelectionModel().clearSelection();
		}

		@Override
		public void addSequence(Macro seq) {
			layoutEditor.refresh();
		}

		@Override
		public void buildTree() {
		}

		@Override
		public MultipleSelectionModel<KeySequence> getSequenceSelectionModel() {
			return sequenceSelectionModel;
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

	class ListMacrosView extends ListView<KeySequence> implements MacrosView {

		MacroBank bank;
		App context;
		private MultipleSelectionModel<KeySequence> sequenceSelectionModel;
		private KeySequence lastSequence;

		ListMacrosView(MacroBank map, App context) {
			this.bank = map;
			this.context = context;

			getStyleClass().add("transparentBackground");
			setCellFactory((list) -> {
				return new MacroListCell();
			});
			getSelectionModel().setSelectionMode(SelectionMode.SINGLE);

			sequenceSelectionModel = new ListMultipleSelectionModel<>(new BasicList<>());
			sequenceSelectionModel.setSelectionMode(SelectionMode.SINGLE);
			getSelectionModel().selectedItemProperty().addListener((a) -> {
				checkSelectionChange();
			});
		}

		protected void checkSelectionChange() {
			KeySequence seqItem = getSelectionModel().getSelectedItem();
			if (!Objects.equals(seqItem, lastSequence)) {
				lastSequence = seqItem;
				sequenceSelectionModel.select(lastSequence);
			}
		}

		public void buildTree() {
			getItems().clear();
			for (Macro macro : bank.getMacros()) {
				getItems().add(macro.getActivatedBy());
			}
		}

		@Override
		public MultipleSelectionModel<KeySequence> getSequenceSelectionModel() {
			return sequenceSelectionModel;
		}

		@Override
		public void addSequence(Macro macro) {
			getItems().add(macro.getActivatedBy());
			getSelectionModel().select(macro.getActivatedBy());
			requestFocus();
		}

		@Override
		public void deleteSelected() {
			lastSequence = null;
			getSelectionModel().clearSelection();
			buildTree();
		}

		@Override
		public Node getNode() {
			return this;
		}
	}
}