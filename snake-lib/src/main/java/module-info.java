module uk.co.bithatch.snake.lib {
    requires transitive org.apache.commons.lang3;
	requires transitive java.prefs;
	requires transitive java.xml;
	requires transitive com.google.gson;
	requires jdk.crypto.ec;
    exports uk.co.bithatch.snake.lib;
    exports uk.co.bithatch.snake.lib.effects;
    exports uk.co.bithatch.snake.lib.layouts;
    opens uk.co.bithatch.snake.lib;
    opens uk.co.bithatch.snake.lib.layouts;
    uses uk.co.bithatch.snake.lib.Backend; 
}