module uk.co.bithatch.snake.widgets {
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
	requires transitive javafx.fxml;
	requires transitive com.sshtools.icongenerator.javafx;
    exports uk.co.bithatch.snake.widgets;
    opens uk.co.bithatch.snake.widgets;
}
