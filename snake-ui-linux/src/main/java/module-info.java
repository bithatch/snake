module uk.co.bithatch.snake.ui.linux {
	requires transitive uk.co.bithatch.snake.ui;
	provides uk.co.bithatch.snake.ui.PlatformService with uk.co.bithatch.snake.ui.linux.LinuxPlatformService;
}