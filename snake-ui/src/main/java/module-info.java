module uk.co.bithatch.snake.ui {
	requires java.desktop;
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
	requires transitive javafx.fxml;
	requires jdk.zipfs;
	requires transitive uk.co.bithatch.snake.lib;
	requires transitive javafx.web;
	requires com.goxr3plus.fxborderlessscene;
	requires SystemTray;
    exports uk.co.bithatch.snake.ui;
    exports uk.co.bithatch.snake.ui.addons;
    exports uk.co.bithatch.snake.ui.effects;
    exports uk.co.bithatch.snake.ui.util;
    exports uk.co.bithatch.snake.ui.designer;
    exports uk.co.bithatch.snake.ui.widgets;
    opens uk.co.bithatch.snake.ui;
    opens uk.co.bithatch.snake.ui.icons; 
    opens uk.co.bithatch.snake.ui.designer;
	requires com.sshtools.forker.wrapped;
	requires transitive org.commonmark;
	requires transitive com.sshtools.icongenerator.common;
	requires javafx.base;
	requires transitive org.codehaus.groovy;
	requires transitive uk.co.bithatch.linuxio;
	requires com.sshtools.twoslices;
	requires org.controlsfx.controls;
    uses uk.co.bithatch.snake.lib.Backend; 
    uses uk.co.bithatch.snake.ui.PlatformService; 
    uses uk.co.bithatch.snake.ui.EffectHandler;
	provides uk.co.bithatch.snake.ui.EffectHandler with uk.co.bithatch.snake.ui.effects.BreathEffectHandler,
	uk.co.bithatch.snake.ui.effects.OnEffectHandler,
	uk.co.bithatch.snake.ui.effects.OffEffectHandler,
	uk.co.bithatch.snake.ui.effects.PulsateEffectHandler,
	uk.co.bithatch.snake.ui.effects.ReactiveEffectHandler,
	uk.co.bithatch.snake.ui.effects.RippleEffectHandler,
	uk.co.bithatch.snake.ui.effects.SpectrumEffectHandler,
	uk.co.bithatch.snake.ui.effects.StarlightEffectHandler,
	uk.co.bithatch.snake.ui.effects.StaticEffectHandler,
	uk.co.bithatch.snake.ui.effects.WaveEffectHandler;
}

