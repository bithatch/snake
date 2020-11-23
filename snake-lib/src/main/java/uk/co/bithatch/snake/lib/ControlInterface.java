package uk.co.bithatch.snake.lib;

import java.util.Set;

import uk.co.bithatch.snake.lib.effects.Effect;

public interface ControlInterface extends AutoCloseable {

	Effect getEffect(Lit lit);

	void setEffect(Lit lit, Effect effect);

	void update(Lit lit);

	Set<Lit> getLitAreas();
}
