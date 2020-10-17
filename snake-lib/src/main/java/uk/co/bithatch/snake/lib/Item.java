package uk.co.bithatch.snake.lib;

import java.util.Set;

import uk.co.bithatch.snake.lib.effects.Effect;

public interface Item {

	Set<Capability> getCapabilities();

	void setBrightness(short brightness);

	Set<Class<? extends Effect>> getSupportedEffects();

	short getBrightness();
}
