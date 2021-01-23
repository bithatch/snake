package uk.co.bithatch.snake.ui.widgets;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import uk.co.bithatch.macrolib.MacroBank;
import uk.co.bithatch.macrolib.MacroDevice;
import uk.co.bithatch.macrolib.MacroProfile;
import uk.co.bithatch.macrolib.MacroSystem;
import uk.co.bithatch.macrolib.MacroSystem.ProfileListener;
import uk.co.bithatch.snake.widgets.ProfileLEDs;

public class MacroBankLEDHelper implements Closeable, ProfileListener, ChangeListener<boolean[]> {

	private ProfileLEDs profileLEDs;
	private MacroBank bank;
	private MacroSystem macroSystem;
	private boolean adjusting;
	private boolean automatic;

	public MacroBankLEDHelper(MacroSystem macroSystem, ProfileLEDs profileLEDs) {
		this(macroSystem, null, profileLEDs);
	}

	public MacroBankLEDHelper(MacroSystem macroSystem, MacroBank bank, ProfileLEDs profileLEDs) {
		this.profileLEDs = profileLEDs;
		this.macroSystem = macroSystem;

		macroSystem.addProfileListener(this);
		profileLEDs.rgbs().addListener(this);

		setBank(bank);
	}

	public MacroBank getBank() {
		return bank;
	}

	public void setBank(MacroBank bank) {
		if (!Objects.equals(bank, this.bank)) {
			this.bank = bank;
			updateState();
		}
	}

	@Override
	public void close() throws IOException {
		macroSystem.removeProfileListener(this);
		profileLEDs.rgbs().removeListener(this);
	}

	@Override
	public void profileChanged(MacroDevice device, MacroProfile profile) {
		if (bank != null && bank.getProfile().equals(profile)) {
			updateState();
		}
	}

	public boolean isAutomatic() {
		return automatic;
	}

	public void setAutomatic(boolean automatic) {
		if (this.automatic != automatic) {
			this.automatic = automatic;
			if (automatic)
				bank.getProperties().remove("leds");
			else
				bank.getProperties().put("leds", Arrays.asList(true, true, true));
			bank.commit();
		}
	}

	@Override
	public void changed(ObservableValue<? extends boolean[]> observable, boolean[] oldValue, boolean[] newValue) {
		if (bank != null && !adjusting) {
			if (automatic) {
				bank.getProperties().remove("leds");
			} else {
				bank.getProperties().put("leds", Arrays.asList(newValue[0], newValue[1], newValue[2]));
			}
			bank.commit();
		}
	}

	protected void updateState() {
		adjusting = true;
		try {
			@SuppressWarnings("unchecked")
			List<Boolean> l = (List<Boolean>) bank.getProperties().getOrDefault("leds", null);
			if (l == null) {
				automatic = true;
				profileLEDs.setRgbs(new boolean[] { false, false, false });
			} else {
				automatic = false;
				profileLEDs.setRgbs(new boolean[] { l.get(0), l.get(1), l.get(2) });
			}
		} finally {
			adjusting = false;
		}
	}
}
