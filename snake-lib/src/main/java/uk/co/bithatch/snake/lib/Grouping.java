package uk.co.bithatch.snake.lib;

public interface Grouping extends Item {

	int getBattery();

	boolean isCharging();

	boolean isGameMode();

	void setGameMode(boolean gameMode);
}
