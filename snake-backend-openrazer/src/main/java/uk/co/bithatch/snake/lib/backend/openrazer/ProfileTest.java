package uk.co.bithatch.snake.lib.backend.openrazer;

import java.util.Map;

import uk.co.bithatch.linuxio.EventCode;
import uk.co.bithatch.snake.lib.Backend;
import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.binding.MapSequence;
import uk.co.bithatch.snake.lib.binding.Profile;
import uk.co.bithatch.snake.lib.binding.ProfileMap;

public class ProfileTest {

	public static void main(String[] args) throws Exception {
		try (Backend be = new OpenRazerBackend()) {
			be.init();
			Device device = be.getDevices().get(0);

			for (Profile profile : device.getProfiles()) {
				System.out.println(profile + " " + (profile.isActive() ? "*" : ""));
				for (ProfileMap map : profile.getMaps()) {
					for (Map.Entry<EventCode, MapSequence> en : map.getSequences().entrySet()) {
						System.out.println("          " + en.getKey() + " : " + en.getValue());
					}
				}
			}
		}
	}
}
