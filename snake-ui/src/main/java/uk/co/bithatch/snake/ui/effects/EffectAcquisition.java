package uk.co.bithatch.snake.ui.effects;

import java.util.Set;

import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.ui.EffectHandler;

public interface EffectAcquisition extends AutoCloseable {

	public interface EffectChangeListener {
		void effectChanged(Lit component, EffectHandler<?, ?> effect);
	}

	Device getDevice();

	EffectHandler<?, ?> getEffect(Lit lit);

	void activate(Lit lit, EffectHandler<?, ?> effect);

	void update(Lit lit);

	Set<Lit> getLitAreas();

	void addListener(EffectChangeListener listener);

	void removeListener(EffectChangeListener listener);
}
