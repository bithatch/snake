package uk.co.bithatch.snake.ui.effects;

import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Breath;
import uk.co.bithatch.snake.lib.effects.Breath.Mode;
import uk.co.bithatch.snake.ui.BreathOptions;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;

public class BreathEffectHandler extends AbstractBackendEffectHandler<Breath, BreathOptions> {

	public BreathEffectHandler() {
		super(Breath.class, BreathOptions.class);
	}

	@Override
	protected void onLoad(Preferences prefs, Breath effect) {
		effect.setMode(Mode.valueOf(prefs.get("mode", Mode.RANDOM.name())));
		effect.setColor(Colors.fromHex(prefs.get("color", "#00ff00")));
		effect.setColor1(Colors.fromHex(prefs.get("color2", "#00ff00")));
		effect.setColor2(Colors.fromHex(prefs.get("color1", "#0000ff")));
	}

	@Override
	protected void onSave(Preferences prefs, Breath effect) {
		prefs.put("mode", effect.getMode().name());
		prefs.put("color", Colors.toHex(effect.getColor()));
		prefs.put("color1", Colors.toHex(effect.getColor1()));
		prefs.put("color2", Colors.toHex(effect.getColor2()));
	}

	@Override
	protected void onStore(Lit component, BreathOptions controller) throws Exception {
		Breath breath = (Breath) controller.getEffect().clone();
		breath.setMode(controller.getMode());
		breath.setColor(controller.getColor());
		breath.setColor1(controller.getColor1());
		breath.setColor2(controller.getColor2());
		getContext().getSchedulerManager().get(Queue.DEVICE_IO).execute(() -> component.setEffect(breath));
		save(component, breath);
	}

}
