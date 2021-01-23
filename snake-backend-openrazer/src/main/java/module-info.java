module uk.co.bithatch.snake.lib.backend.openrazer {
    requires transitive uk.co.bithatch.snake.lib;
	requires transitive org.jnrproject.ffi;
	requires transitive uk.co.bithatch.linuxio;
	requires transitive org.freedesktop.dbus; 
	provides uk.co.bithatch.snake.lib.Backend with uk.co.bithatch.snake.lib.backend.openrazer.OpenRazerBackend;
	exports uk.co.bithatch.snake.lib.backend.openrazer;
}