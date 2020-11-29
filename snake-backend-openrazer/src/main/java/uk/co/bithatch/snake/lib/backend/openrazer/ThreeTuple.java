package uk.co.bithatch.snake.lib.backend.openrazer;

import org.freedesktop.dbus.Tuple;

public final class ThreeTuple<A, B, C> extends Tuple {
	public final A a;
	public final B b;
	public final C c;

	public ThreeTuple(A a, B b, C c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}
}