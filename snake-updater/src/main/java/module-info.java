module uk.co.bithatch.snake.updater {
	requires java.desktop;
	requires java.prefs;
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
	requires transitive javafx.fxml;
	requires transitive com.sshtools.forker.updater;
	requires transitive com.sshtools.forker.wrapper;
	requires transitive com.goxr3plus.fxborderlessscene;
    exports uk.co.bithatch.snake.updater;
    opens uk.co.bithatch.snake.updater; 
    opens uk.co.bithatch.snake.updater.icons;
	provides com.sshtools.forker.updater.UpdateHandler with uk.co.bithatch.snake.updater.JavaFXUpdateHandler;
	provides com.sshtools.forker.updater.InstallHandler with uk.co.bithatch.snake.updater.JavaFXInstallHandler;
}