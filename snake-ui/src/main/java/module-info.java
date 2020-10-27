module uk.co.bithatch.snake.ui {
	requires java.desktop;
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
	requires transitive javafx.fxml;
	requires transitive uk.co.bithatch.snake.lib;
	requires transitive javafx.web;
	requires com.goxr3plus.fxborderlessscene;
	requires SystemTray;
    exports uk.co.bithatch.snake.ui;
    opens uk.co.bithatch.snake.ui;
    opens uk.co.bithatch.snake.ui.icons;  
	requires com.sshtools.forker.wrapped;
	requires transitive org.commonmark;
	requires transitive com.sshtools.icongenerator.common;
	requires javafx.base;
	requires transitive org.codehaus.groovy;
    uses uk.co.bithatch.snake.lib.Backend; 
    uses uk.co.bithatch.snake.ui.PlatformService; 
    uses uk.co.bithatch.snake.ui.EffectOptions;
	provides uk.co.bithatch.snake.ui.EffectOptions with uk.co.bithatch.snake.ui.WaveEffectOptions, 
		uk.co.bithatch.snake.ui.BreathEffectOptions, 
		uk.co.bithatch.snake.ui.StaticEffectOptions, 
		uk.co.bithatch.snake.ui.ReactiveEffectOptions, 
		uk.co.bithatch.snake.ui.MatrixEffectOptions, 
		uk.co.bithatch.snake.ui.RippleEffectOptions, 
		uk.co.bithatch.snake.ui.StarlightEffectOptions; 
}

