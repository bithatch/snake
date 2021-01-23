package uk.co.bithatch.snake.ui.effects;

import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Static;
import uk.co.bithatch.snake.ui.SchedulerManager.Queue;
import uk.co.bithatch.snake.ui.StaticOptions;

public class StaticEffectHandler extends AbstractBackendEffectHandler<Static, StaticOptions> {

	public StaticEffectHandler() {
		super(Static.class, StaticOptions.class);
	}

	@Override
	protected void onLoad(Preferences prefs, Static effect) {
		effect.setColor(Colors.fromHex(prefs.get("color", "#00ff00")));
	}

	@Override
	protected void onSave(Preferences prefs, Static effect) {
		prefs.put("color", Colors.toHex(effect.getColor()));
	}

	@Override
	protected void onStore(Lit component, StaticOptions controller) throws Exception {
		Static staticEffect = (Static) controller.getEffect().clone();
		staticEffect.setColor(controller.getColor());
		getContext().getSchedulerManager().get(Queue.DEVICE_IO).execute(() -> component.setEffect(staticEffect));
		save(component, staticEffect);

	}

}
