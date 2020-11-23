package uk.co.bithatch.snake.ui.effects;

import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Starlight;
import uk.co.bithatch.snake.lib.effects.Starlight.Mode;
import uk.co.bithatch.snake.ui.StarlightOptions;

public class StarlightEffectHandler extends AbstractBackendEffectHandler<Starlight, StarlightOptions> {

	public StarlightEffectHandler() {
		super(Starlight.class, StarlightOptions.class);
	}

	@Override
	protected void onLoad(Preferences prefs, Starlight effect) {
		effect.setMode(Mode.valueOf(prefs.get("mode", Mode.RANDOM.name())));
		effect.setColor(Colors.fromHex(prefs.get("color", "#00ff00")));
		effect.setColor1(Colors.fromHex(prefs.get("color2", "#00ff00")));
		effect.setColor2(Colors.fromHex(prefs.get("color1", "#0000ff")));
		effect.setSpeed(prefs.getInt("speed", 100));
	}

	@Override
	protected void onSave(Preferences prefs, Starlight effect) {
		prefs.put("mode", effect.getMode().name());
		prefs.put("color", Colors.toHex(effect.getColor()));
		prefs.put("color1", Colors.toHex(effect.getColor1()));
		prefs.put("color2", Colors.toHex(effect.getColor2()));
		prefs.putInt("speed", effect.getSpeed());
	}

	@Override
	protected void onStore(Lit component, StarlightOptions controller) throws Exception {
		Starlight starlight = (Starlight) controller.getEffect().clone();
		starlight.setMode(controller.getMode());
		starlight.setColor(controller.getColor());
		starlight.setColor1(controller.getColor1());
		starlight.setColor2(controller.getColor2());
		starlight.setSpeed(controller.getSpeed());
		getContext().getScheduler().execute(() -> component.setEffect(starlight));
		save(component, starlight);
		
	}

}
