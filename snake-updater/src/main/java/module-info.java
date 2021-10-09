module uk.co.bithatch.snake.updater {
	requires java.desktop;
	requires java.prefs;
	requires java.logging;
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
	requires transitive javafx.fxml;
	requires transitive com.sshtools.forker.updater;
	requires transitive com.sshtools.forker.wrapper;
	requires transitive com.goxr3plus.fxborderlessscene;
    exports uk.co.bithatch.snake.updater;
    opens uk.co.bithatch.snake.updater; 
    opens uk.co.bithatch.snake.updater.icons;
	requires transitive org.kordamp.ikonli.fontawesome;
	requires transitive org.kordamp.ikonli.javafx;
	provides com.sshtools.forker.updater.UpdateHandler with uk.co.bithatch.snake.updater.JavaFXUpdateHandler;
	provides com.sshtools.forker.updater.InstallHandler with uk.co.bithatch.snake.updater.JavaFXInstallHandler;
	provides com.sshtools.forker.updater.UninstallHandler with uk.co.bithatch.snake.updater.JavaFXUninstallHandler;
}