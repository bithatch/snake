package uk.co.bithatch.snake.ui;

import java.net.URL;

import uk.co.bithatch.snake.lib.DeviceType;
import uk.co.bithatch.snake.lib.Region.Name;
import uk.co.bithatch.snake.lib.effects.Effect;

public class Images {

	public static URL getEffectImage(int size, Class<? extends Effect> effect) {
		return checkResource(effect.getClass().getName(),
				App.class.getResource("effects/" + effect.getSimpleName().toLowerCase() + size + ".png"));
	}

	public static URL getDeviceImage(int size, DeviceType type) {
		return checkResource(type, App.class.getResource("devices/" + type.name().toLowerCase() + size + ".png"));
	}

	public static URL getRegionImage(int size, Name region) {
		return checkResource(region, App.class.getResource("regions/" + region.name().toLowerCase() + size + ".png"));
	}

	static URL checkResource(Object ctx, URL url) {
		if (url == null)
			throw new IllegalArgumentException(String.format("Image for %s does not exist.", String.valueOf(ctx)));
		return url;
	}
}
