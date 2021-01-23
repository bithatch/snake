package uk.co.bithatch.snake.ui.effects;

import java.util.Set;

import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Lit;
import uk.co.bithatch.snake.lib.effects.Effect;
import uk.co.bithatch.snake.ui.EffectHandler;

public interface EffectAcquisition extends AutoCloseable {

	public interface EffectChangeListener {
		void effectChanged(Lit component, EffectHandler<?, ?> effect);
	}

	<E extends Effect> E getEffectInstance(Lit component);

	Device getDevice();

	EffectHandler<?, ?> getEffect(Lit lit);

	void activate(Lit lit, EffectHandler<?, ?> effect);

	void deactivate(Lit lit);

	void remove(Lit lit);

	void update(Lit lit);

	Set<Lit> getLitAreas();

	void addListener(EffectChangeListener listener);

	void removeListener(EffectChangeListener listener);

	<E extends EffectHandler<?, ?>> E activate(Lit lit, Class<E> class1);
}
