package uk.co.bithatch.snake.ui;

import java.text.MessageFormat;
import java.util.ResourceBundle;

import com.sshtools.twoslices.Toast;
import com.sshtools.twoslices.ToastType;

import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.lib.Device.Listener;
import uk.co.bithatch.snake.lib.Region;
import uk.co.bithatch.snake.lib.binding.Profile;
import uk.co.bithatch.snake.lib.binding.ProfileMap;

public class DesktopNotifications implements Listener {

	final static ResourceBundle bundle = ResourceBundle.getBundle(DesktopNotifications.class.getName());

	private App context;

	public DesktopNotifications(App context) throws Exception {
		this.context = context;
		for (Device d : context.getBackend().getDevices()) {
			d.addListener(this);
		}
	}

	@Override
	public void changed(Device device, Region region) {
	}

	@Override
	public void activeMapChanged(ProfileMap map) {
		String cpath = context.getCache()
				.getCachedImage(context.getDefaultImage(map.getProfile().getDevice().getType(),
						map.getProfile().getDevice().getImage()));
		if(cpath.startsWith("file:"))
			cpath = cpath.substring(5);
		Toast.toast(ToastType.INFO,
				cpath,
				MessageFormat.format(bundle.getString("profileMapChange.title"), map.getProfile().getName(),
						map.getId()),
				MessageFormat.format(bundle.getString("profileMapChange"), map.getProfile().getName(), map.getId()));
	}

	@Override
	public void profileAdded(Profile profile) {
	}

	@Override
	public void profileRemoved(Profile profile) {
	}

	@Override
	public void mapAdded(ProfileMap profile) {
	}

	@Override
	public void mapChanged(ProfileMap profile) {
	}

	@Override
	public void mapRemoved(ProfileMap profile) {
	}
}
