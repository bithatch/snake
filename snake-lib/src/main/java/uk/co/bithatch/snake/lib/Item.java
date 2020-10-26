package uk.co.bithatch.snake.lib;

import java.util.Set;

import uk.co.bithatch.snake.lib.effects.Effect;

public interface Item {

	short getBrightness();

	Set<Capability> getCapabilities();

	Set<Class<? extends Effect>> getSupportedEffects();

	void setBrightness(short brightness);
}
