package uk.co.bithatch.snake.ui.macros;

import java.io.IOException;
import java.util.Iterator;
import java.util.UUID;

import uk.co.bithatch.macrolib.JsonMacroStorage;
import uk.co.bithatch.macrolib.MacroDevice;
import uk.co.bithatch.macrolib.MacroProfile;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.addons.Macros;
import uk.co.bithatch.snake.ui.util.CompoundIterator;

/**
 * Augment user profiles with read-only profiles shared by others and installed
 * as add-ons.
 */
public class MacrolibStorage extends JsonMacroStorage {

	private App context;

	public MacrolibStorage(App context) {
		this.context = context;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Iterator<MacroProfile> profiles(MacroDevice device) throws IOException {
		return new CompoundIterator<>(super.profiles(device), iterateAddOnProfiles(device));
	}

	@Override
	public MacroProfile loadProfile(MacroDevice device, UUID id) throws IOException {
		for (Macros macros : context.getAddOnManager().getAddOns(Macros.class)) {
			if (macros.getProfile().getId().equals(id)) {
				return macros.getProfile();
			}
		}
		return super.loadProfile(device, id);
	}

	private Iterator<MacroProfile> iterateAddOnProfiles(MacroDevice device) throws IOException {
		Iterator<Macros> macrosIt = context.getAddOnManager().getAddOns(Macros.class).iterator();
		return new Iterator<MacroProfile>() {
			@Override
			public boolean hasNext() {
				return macrosIt.hasNext();
			}

			@Override
			public MacroProfile next() {
				return macrosIt.next().getProfile();
			}

		};
	}

}
