module uk.co.bithatch.snake.ui {
	requires java.desktop;
    requires transitive javafx.controls;
    requires transitive javafx.graphics;
	requires transitive javafx.fxml;
	requires transitive javafx.swing;
	requires jdk.zipfs;
	requires transitive uk.co.bithatch.snake.lib;
	requires transitive javafx.web;
	requires com.goxr3plus.fxborderlessscene;
	requires kotlin.stdlib;
	requires SystemTray;
	requires Updates;
	requires transitive uk.co.bithatch.snake.widgets;
    exports uk.co.bithatch.snake.ui;
    exports uk.co.bithatch.snake.ui.addons;
    exports uk.co.bithatch.snake.ui.effects;
    exports uk.co.bithatch.snake.ui.util;
    exports uk.co.bithatch.snake.ui.designer;
    exports uk.co.bithatch.snake.ui.widgets;
    exports uk.co.bithatch.snake.ui.macros;
    exports uk.co.bithatch.snake.ui.drawing;
    exports uk.co.bithatch.snake.ui.audio;
    exports uk.co.bithatch.snake.ui.tray;
    opens uk.co.bithatch.snake.ui;
    opens uk.co.bithatch.snake.ui.icons; 
    opens uk.co.bithatch.snake.ui.designer;
    opens uk.co.bithatch.snake.ui.widgets;
    opens uk.co.bithatch.snake.ui.tray;
	requires transitive com.sshtools.forker.wrapped;
	requires transitive org.commonmark;
	requires transitive com.sshtools.icongenerator.common;
	requires javafx.base;
	requires transitive org.codehaus.groovy;
	requires transitive uk.co.bithatch.linuxio;
	requires com.sshtools.twoslices;
	requires org.controlsfx.controls;
	requires transitive uk.co.bithatch.macrolib;
	requires uk.co.bithatch.jimpulse;
	requires transitive uk.co.bithatch.jdraw;
	requires transitive com.sshtools.jfreedesktop.javafx;
	requires transitive org.kordamp.ikonli.fontawesome;
	requires transitive org.kordamp.ikonli.javafx;
	requires transitive org.kordamp.ikonli.swing;
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
	uk.co.bithatch.snake.ui.effects.WaveEffectHandler,
	uk.co.bithatch.snake.ui.effects.AudioEffectHandler;
}
