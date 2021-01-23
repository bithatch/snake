package uk.co.bithatch.snake.ui;

import java.io.IOException;
import java.lang.System.Logger;
import java.util.Iterator;
import java.util.ResourceBundle;

import org.controlsfx.control.SearchableComboBox;
import org.kordamp.ikonli.fontawesome.FontAwesome;
import org.kordamp.ikonli.javafx.FontIcon;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import uk.co.bithatch.macrolib.MacroBank;
import uk.co.bithatch.macrolib.MacroDevice;
import uk.co.bithatch.macrolib.MacroProfile;
import uk.co.bithatch.macrolib.MacroSystem;
import uk.co.bithatch.macrolib.MacroSystem.ActiveBankListener;
import uk.co.bithatch.macrolib.MacroSystem.ActiveProfileListener;
import uk.co.bithatch.macrolib.MacroSystem.MacroSystemListener;
import uk.co.bithatch.macrolib.MacroSystem.ProfileListener;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.widgets.Direction;
import uk.co.bithatch.snake.widgets.GeneratedIcon;
import uk.co.bithatch.snake.widgets.JavaFX;

public class MacroProfileControl extends ControlController
		implements ActiveBankListener, ActiveProfileListener, ProfileListener, MacroSystemListener {

	final static Logger LOG = System.getLogger(MacroProfileControl.class.getName());

	final static ResourceBundle bundle = ResourceBundle.getBundle(MacroProfileControl.class.getName());

	@FXML
	private SearchableComboBox<MacroProfile> profiles;
	@FXML
	private VBox banks;
	@FXML
	private Hyperlink addProfile;
	@FXML
	private Hyperlink configure;

	private MacroSystem macroSystem;

	private boolean adjusting;

	@Override
	protected void onDeviceCleanUp() {
		macroSystem.removeActiveBankListener(this);
		macroSystem.removeProfileListener(this);
		macroSystem.removeActiveProfileListener(this);
		macroSystem.removeMacroSystemListener(this);
	}

	@Override
	protected void onSetControlDevice() {
		macroSystem = context.getMacroManager().getMacroSystem();
		buildProfiles();
		buildBanks();
		configureForProfile();
		macroSystem.addActiveBankListener(this);
		macroSystem.addProfileListener(this);
		macroSystem.addActiveProfileListener(this);
		macroSystem.addMacroSystemListener(this);

		Callback<ListView<MacroProfile>, ListCell<MacroProfile>> factory = listView -> {
			return new ListCell<MacroProfile>() {
				@Override
				protected void updateItem(MacroProfile item, boolean empty) {
					super.updateItem(item, empty);
					if (item == null || empty) {
						setText("");
					} else {
						setText(item.getName());
					}
				}
			};
		};
		profiles.setButtonCell(factory.call(null));
		profiles.setCellFactory(factory);
		profiles.getSelectionModel().select(macroSystem.getActiveProfile(getMacroDevice()));
		profiles.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
			if (n != null && !adjusting) {
				try {
					n.makeActive();
				} catch (IOException e1) {
					error(e1);
				}
			}
		});
	}

	protected void buildProfiles() {
		MacroProfile sel = profiles.getSelectionModel().getSelectedItem();
		profiles.getSelectionModel().clearSelection();
		profiles.getItems().clear();
		try {
			MacroDevice macroDevice = getMacroDevice();
			for (Iterator<MacroProfile> profileIt = macroSystem.profiles(macroDevice); profileIt.hasNext();) {
				MacroProfile profile = profileIt.next();
				profiles.getItems().add(profile);
			}
			if (profiles.getItems().contains(sel))
				profiles.getSelectionModel().select(sel);
		} catch (Exception e) {
			error(e);
		}
	}

	protected void buildBanks() {
		banks.getChildren().clear();
		try {
			MacroDevice macroDevice = getMacroDevice();
			MacroProfile activeProfile = macroSystem.getActiveProfile(macroDevice);
			for (Iterator<MacroProfile> profileIt = macroSystem.profiles(macroDevice); profileIt.hasNext();) {
				MacroProfile profile = profileIt.next();
				if (profile.equals(activeProfile)) {
					Iterator<MacroBank> macroBanks = profile.getBanks().iterator();
					for (int i = 0; i < macroDevice.getBanks(); i++) {
						MacroBank bank = macroBanks.hasNext() ? macroBanks.next() : null;
						banks.getChildren().add(new BankRow(i, getDevice(), bank));
					}
				}
			}
		} catch (Exception e) {
			error(e);
		}
	}

	protected void addBank(MacroProfile profile) {
		int nextFreeBankNumber;
		try {
			nextFreeBankNumber = profile.nextFreeBankNumber();
		} catch (IllegalStateException ise) {
			error(ise);
			return;
		}
		Input confirm = context.push(Input.class, Direction.FADE);
		confirm.getConfirm().setGraphic(new FontIcon(FontAwesome.PLUS_CIRCLE));
		confirm.setValidator((l) -> {
			String name = confirm.inputProperty().get();
			boolean available = profile.getBank(name) == null;
			l.getStyleClass().clear();
			if (available) {
				l.textProperty().set("");
				l.visibleProperty().set(false);
			} else {
				l.visibleProperty().set(true);
				l.textProperty().set(bundle.getString("error.bankWithNameAlreadyExists"));
				l.getStyleClass().add("danger");
			}
			return available;
		});
		confirm.confirm(bundle, "addBank", () -> {
			profile.createBank(confirm.inputProperty().get());
		}, String.valueOf(nextFreeBankNumber + 1));
	}

	@FXML
	void evtConfigure() throws Exception {
		context.editMacros(this);
	}

	void configureForProfile() {
		MacroDevice device = getMacroDevice();
		MacroBank profileBank = macroSystem.getActiveBank(device);

		/* Selected profile */
		for (Node n : banks.getChildren()) {
			((MacrosComponent) n).updateAvailability();
		}

		configure.disableProperty().set(profileBank == null);
	}

	protected MacroDevice getMacroDevice() {
		return context.getMacroManager().getMacroDevice(getDevice());
	}

	protected void remove(MacroBank bank) {
		Confirm confirm = context.push(Confirm.class, Direction.FADE);
		confirm.getYes().setGraphic(new FontIcon(FontAwesome.TRASH));
		confirm.confirm(bundle, "removeBank", () -> {
			try {
				bank.remove();
			} catch (IOException e) {
				error(e);
			}
		}, null, bank.getBank() + 1, bank.getDisplayName());
	}

	@FXML
	void evtAddProfile() {
		Input confirm = context.push(Input.class, Direction.FADE);
		confirm.setValidator((l) -> {
			String name = confirm.inputProperty().get();
			boolean available = macroSystem.getProfileWithName(getMacroDevice(), name) == null;
			l.getStyleClass().clear();
			if (available) {
				l.textProperty().set("");
				l.visibleProperty().set(false);
			} else {
				l.visibleProperty().set(true);
				l.textProperty().set(bundle.getString("error.nameAlreadyExists"));
				l.getStyleClass().add("danger");
			}
			return available;
		});
		confirm.confirm(bundle, "addProfile", () -> {
			macroSystem.createProfile(getMacroDevice(), confirm.inputProperty().get());
		});
	}

	interface MacrosComponent {
		void updateAvailability();
	}

	class BankRow extends HBox implements MacrosComponent {

		MacroBank bank;
		Hyperlink link;
		Device device;
		GeneratedIcon icon;

		BankRow(int number, Device device, MacroBank bank) {
			this.bank = bank;
			this.device = device;
			getStyleClass().add("small");
			getStyleClass().add("gapLeft");

			setAlignment(Pos.CENTER_LEFT);

			icon = new GeneratedIcon();
			icon.setPrefHeight(22);
			icon.setPrefWidth(22);
			icon.setText(String.valueOf(number + 1));
			if (bank == null) {
				link = new Hyperlink(bundle.getString("empty"));
				link.setOnAction((e) -> {
					addBank(profiles.getSelectionModel().getSelectedItem());
				});
				link.getStyleClass().add("deemphasis");
				icon.getStyleClass().add("deemphasis");
			} else {
				icon.getStyleClass().add("filled");
				link = new Hyperlink(bank.getDisplayName());
				link.setOnAction((e) -> {
					try {
						bank.makeActive();
					} catch (Exception ex) {
						error(ex);
					}
				});

				ContextMenu bankContextMenu = new ContextMenu();
				MenuItem remove = new MenuItem(bundle.getString("contextMenuRemove"));
				remove.onActionProperty().set((e) -> {
					remove(bank);
				});
				bankContextMenu.getItems().add(remove);
				bankContextMenu.setOnShowing((e) -> {
					remove.visibleProperty().set(macroSystem.getNumberOfProfiles(getMacroDevice()) > 1
							|| bank.getProfile().getBanks().size() > 1);
				});
				link.setContextMenu(bankContextMenu);
			}

			getChildren().add(icon);
			getChildren().add(link);

			updateAvailability();
		}

		public void updateAvailability() {
			if (bank != null) {
				JavaFX.glowOrDeemphasis(this, bank.isActive());
				if (bank.isDefault()) {
					if (!getStyleClass().contains("emphasis"))
						getStyleClass().add("emphasis");
				} else {
					getStyleClass().remove("emphasis");
				}
			}
		}
	}

	@Override
	public void profileChanged(MacroDevice device, MacroProfile profile) {
		macroSystemChanged();
	}

	@Override
	public void activeProfileChanged(MacroDevice device, MacroProfile profile) {
		if (Platform.isFxApplicationThread()) {
			adjusting = true;
			try {
				buildBanks();
				configureForProfile();
			} finally {
				adjusting = false;
			}
		} else
			Platform.runLater(() -> activeProfileChanged(device, profile));
	}

	@Override
	public void activeBankChanged(MacroDevice device, MacroBank bank) {
		if (Platform.isFxApplicationThread()) {
			adjusting = true;
			try {
				configureForProfile();
			} finally {
				adjusting = false;
			}
		} else
			Platform.runLater(() -> activeBankChanged(device, bank));
	}

	@Override
	public void macroSystemChanged() {
		if (Platform.isFxApplicationThread()) {
			adjusting = true;
			try {
				buildProfiles();
				buildBanks();
				configureForProfile();
			} finally {
				adjusting = false;
			}
		} else
			Platform.runLater(() -> macroSystemChanged());
	}
}
