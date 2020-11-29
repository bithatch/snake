package uk.co.bithatch.snake.ui;

import java.util.List;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import uk.co.bithatch.snake.lib.Capability;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.binding.Profile;
import uk.co.bithatch.snake.lib.binding.ProfileMap;
import uk.co.bithatch.snake.ui.util.JavaFX;
import uk.co.bithatch.snake.ui.widgets.Direction;
import uk.co.bithatch.snake.ui.widgets.MapProfileLEDs;

public class ProfileControl extends ControlController {

	final static ResourceBundle bundle = ResourceBundle.getBundle(ProfileControl.class.getName());

	@FXML
	private VBox profiles;
	@FXML
	private HBox rgbs;
	@FXML
	private Hyperlink addProfile;
	@FXML
	private Hyperlink setDefault;
	@FXML
	private Hyperlink remove;
	@FXML
	private Hyperlink configure;

	private MapProfileLEDs profileLEDs;

	@Override
	protected void onSetControlDevice() {
		buildProfiles();
		profileLEDs = new MapProfileLEDs(getDevice());
		rgbs.getChildren().add(profileLEDs);
		configureForProfile();
	}

	protected void buildProfiles() {
		profiles.getChildren().clear();
		for (Profile profile : getDevice().getProfiles()) {
			profiles.getChildren().add(new ProfileRow(getDevice(), profile));
			List<ProfileMap> maps = profile.getMaps();
			for (ProfileMap map : maps) {
				profiles.getChildren().add(new MapRow(getDevice(), map));
			}
		}
	}

	protected void addMap(Profile profile) {

		Input confirm = context.push(Input.class, Direction.FADE);
		confirm.setValidator((l) -> {
			String name = confirm.inputProperty().get();
			boolean available = profile.getMap(name) == null;
			l.getStyleClass().clear();
			if (available) {
				l.textProperty().set("");
				l.visibleProperty().set(false);
			} else {
				l.visibleProperty().set(true);
				l.textProperty().set(bundle.getString("error.mapWithIDAlreadyExists"));
				l.getStyleClass().add("danger");
			}
			return available;
		});
		confirm.confirm(bundle, "addMap", () -> {
			profile.addMap(confirm.inputProperty().get());
		});
	}

	@FXML
	void evtConfigure() throws Exception {
		MacroMap map = context.push(MacroMap.class, Direction.FROM_BOTTOM);
		map.setMap(getDevice().getActiveProfile().getActiveMap());
	}

	void configureForProfile() {
		Profile profile = getDevice().getActiveProfile();
		ProfileMap profileMap = profile.getActiveMap();

		/* Selected profile */
		for (Node n : profiles.getChildren()) {
			((MapComponent) n).updateAvailability();
		}

		/* RGBs */
		if (getDevice().getCapabilities().contains(Capability.MACRO_PROFILE_LEDS)) {
			ProfileMap map = profile.getActiveMap();
			boolean[] rgb = map.getLEDs();
			profileLEDs.setRGB(rgb);
		}

		setDefault.disableProperty().set(profileMap == null || profileMap.isDefault());
		remove.disableProperty().set(getDevice().getProfiles().size() < 2 && profile.getMaps().size() < 2);
		configure.disableProperty().set(profileMap == null);
	}

	@FXML
	void evtRemove() {
		ProfileMap activeMap = getDevice().getActiveProfile().getActiveMap();
		Confirm confirm = context.push(Confirm.class, Direction.FADE);
		confirm.confirm(bundle, "removeMap", () -> {
			/*
			 * TODO If the profile has other maps, select one of those. If this is the last
			 * map, select another profile. Do this before actually removing due to bug in
			 * the backend.
			 * 
			 * Also make the chosen profile / map the default if this map is currently the
			 * default
			 */
			if (activeMap.getProfile().getMaps().size() == 1) {
				/* Last map, select another profile */
				for (Profile p : getDevice().getProfiles()) {
					if (!p.equals(activeMap.getProfile())) {
						p.activate();
						if (activeMap.isDefault()) {
							p.getActiveMap().makeDefault();
						}
						break;
					}
				}
			} else {
				/* Other maps available */
				for (ProfileMap m : activeMap.getProfile().getMaps()) {
					if (!m.equals(activeMap)) {
						if (activeMap.isDefault()) {
							m.makeDefault();
						}
						m.activate();
						break;
					}
				}
			}

			activeMap.remove();
		}, null, activeMap.getId());
	}

	@FXML
	void evtSetDefault() {
		getDevice().getActiveProfile().getActiveMap().makeDefault();
	}

	@FXML
	void evtAddProfile() {
		Input confirm = context.push(Input.class, Direction.FADE);
		confirm.setValidator((l) -> {
			String name = confirm.inputProperty().get();
			boolean available = getDevice().getProfile(name) == null;
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
			getDevice().addProfile(confirm.inputProperty().get());
		});
	}

	interface MapComponent {
		void updateAvailability();
	}

	class MapRow extends HBox implements MapComponent {

		Label defaultMap;
		ProfileMap profileMap;
		Hyperlink link;
		Device device;

		MapRow(Device device, ProfileMap profileMap) {
			this.profileMap = profileMap;
			this.device = device;
			getStyleClass().add("small");
			getStyleClass().add("gapLeft");

			setAlignment(Pos.CENTER_LEFT);

			defaultMap = new Label(bundle.getString("defaultMap"));
			link = new Hyperlink(profileMap.getId());
			link.setOnAction((e) -> {
				profileMap.activate();
			});

			getChildren().add(defaultMap);
			getChildren().add(link);

			updateAvailability();
		}

		public void updateAvailability() {
			defaultMap.visibleProperty().set(profileMap.isDefault());
			JavaFX.glowOrDeemphasis(this, profileMap.isActive());
			if (profileMap.isDefault()) {
				if (!getStyleClass().contains("emphasis"))
					getStyleClass().add("emphasis");
			} else {
				getStyleClass().remove("emphasis");
			}
		}
	}

	class ProfileRow extends HBox implements MapComponent {

		Profile profile;
		Hyperlink link;
		Device device;
		Hyperlink addMap;

		ProfileRow(Device device, Profile profile) {
			this.profile = profile;
			this.device = device;

			setAlignment(Pos.CENTER_LEFT);

			link = new Hyperlink(profile.getName());
			link.setOnAction((e) -> {
				profile.activate();
			});

			addMap = new Hyperlink(bundle.getString("addMap"));
			addMap.setOnAction((e) -> {
				addMap(profile);
			});
			addMap.setTooltip(JavaFX.quickTooltip(bundle.getString("addMap.tooltip")));

			getChildren().add(link);
			getChildren().add(addMap);

			updateAvailability();
		}

		public void updateAvailability() {
			JavaFX.glowOrDeemphasis(this, profile.isActive());
		}
	}

	@Override
	public void mapChanged(ProfileMap map) {
		if (Platform.isFxApplicationThread()) {
			configureForProfile();
		} else
			Platform.runLater(() -> mapChanged(map));
	}

	@Override
	public void activeMapChanged(ProfileMap map) {
		if (Platform.isFxApplicationThread()) {
			configureForProfile();
		} else
			Platform.runLater(() -> activeMapChanged(map));
	}

	@Override
	public void profileAdded(Profile profile) {
		change(profile);
	}

	@Override
	public void profileRemoved(Profile profile) {
		change(profile);
	}

	@Override
	public void mapAdded(ProfileMap profile) {
		change(profile.getProfile());
	}

	@Override
	public void mapRemoved(ProfileMap profile) {
		change(profile.getProfile());
	}

	void change(Profile profile) {
		if (Platform.isFxApplicationThread()) {
			buildProfiles();
			configureForProfile();
		} else
			Platform.runLater(() -> change(profile));
	}
}
