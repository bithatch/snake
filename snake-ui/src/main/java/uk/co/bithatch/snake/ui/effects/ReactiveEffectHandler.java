package uk.co.bithatch.snake.ui.effects;

import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Colors;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Reactive;
import uk.co.bithatch.snake.ui.ReactiveOptions;

public class ReactiveEffectHandler extends AbstractBackendEffectHandler<Reactive, ReactiveOptions> {

	public ReactiveEffectHandler() {
		super(Reactive.class, ReactiveOptions.class);
	}

	@Override
	protected void onLoad(Preferences prefs, Reactive effect) {
		effect.setSpeed(prefs.getInt("speed", 100));
		effect.setColor(Colors.fromHex(prefs.get("color", "#00ff00")));
	}

	@Override
	protected void onSave(Preferences prefs, Reactive effect) {
		prefs.putInt("speed", effect.getSpeed());
		prefs.put("color", Colors.toHex(effect.getColor()));
	}

	@Override
	protected void onStore(Lit component, ReactiveOptions controller) throws Exception {
		Reactive reactive = (Reactive) controller.getEffect().clone();
		reactive.setColor(controller.getColor());
		reactive.setSpeed(controller.getSpeed());
		getContext().getScheduler().execute(() -> component.setEffect(reactive));
		save(component, reactive);

	}

}
