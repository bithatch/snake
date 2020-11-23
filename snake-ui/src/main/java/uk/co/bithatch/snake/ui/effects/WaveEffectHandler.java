package uk.co.bithatch.snake.ui.effects;

import java.util.prefs.Preferences;

import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Wave;
import uk.co.bithatch.snake.lib.effects.Wave.Direction;
import uk.co.bithatch.snake.ui.WaveOptions;

public class WaveEffectHandler extends AbstractBackendEffectHandler<Wave, WaveOptions> {

	public WaveEffectHandler() {
		super(Wave.class, WaveOptions.class);
	}

	@Override
	protected void onLoad(Preferences prefs, Wave effect) {
		effect.setDirection(Direction.valueOf(prefs.get("direction", Direction.FORWARD.name())));
	}

	@Override
	protected void onSave(Preferences prefs, Wave effect) {
		prefs.put("direction", effect.getDirection().name());
	}

	@Override
	protected void onStore(Lit component, WaveOptions controller) throws Exception {
		Wave wave = (Wave) controller.getEffect().clone();
		Direction dir = controller.getDirection();
		wave.setDirection(dir);
		getContext().getScheduler().execute(() -> component.setEffect(wave));
		save(component, wave);
	}

}
