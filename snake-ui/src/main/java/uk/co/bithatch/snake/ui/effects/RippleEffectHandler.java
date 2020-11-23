package uk.co.bithatch.snake.ui.effects;

import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Ripple;
import uk.co.bithatch.snake.lib.effects.Ripple.Mode;
import uk.co.bithatch.snake.ui.RippleOptions;

public class RippleEffectHandler extends AbstractBackendEffectHandler<Ripple, RippleOptions> {

	public RippleEffectHandler() {
		super(Ripple.class, RippleOptions.class);
	}

	@Override
	protected void onLoad(Preferences prefs, Ripple effect) {
		effect.setMode(Mode.valueOf(prefs.get("mode", Mode.RANDOM.name())));
		effect.setRefreshRate(prefs.getDouble("refreshRate", 100));
		effect.setColor(Colors.fromHex(prefs.get("color", "#00ff00")));
	}

	@Override
	protected void onSave(Preferences prefs, Ripple effect) {
		prefs.put("mode", effect.getMode().name());
		prefs.putDouble("refreshRate", effect.getRefreshRate());
		prefs.put("color", Colors.toHex(effect.getColor()));
	}

	@Override
	protected void onStore(Lit component, RippleOptions controller) throws Exception {
		Ripple ripple = (Ripple) controller.getEffect().clone();
		ripple.setMode(controller.getMode());
		ripple.setColor(controller.getColor());
		ripple.setRefreshRate(controller.getRefreshRate());
		getContext().getScheduler().execute(() -> component.setEffect(ripple));
		save(component, ripple);
		
	}

}
