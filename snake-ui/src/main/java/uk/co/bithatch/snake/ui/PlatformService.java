package uk.co.bithatch.snake.ui;

import java.io.IOException;
import java.util.ServiceLoader;

public interface PlatformService {

	public static boolean isPlatformSupported() {
		ServiceLoader<PlatformService> ps = ServiceLoader.load(PlatformService.class);
		return ps.findFirst().isPresent();
	}

	public static PlatformService get() {
		ServiceLoader<PlatformService> ps = ServiceLoader.load(PlatformService.class);
		return ps.findFirst().get();
	}

	boolean isStartOnLogin();

	void setStartOnLogin(boolean startOnLogin) throws IOException;

	boolean isUpdateableApp();

	boolean isUpdateAvailable();

	boolean isUpdateAutomatically();

	boolean isCheckForUpdates();

	boolean isBetas();

	void setUpdateAutomatically(boolean updateAutomatically);

	void setCheckForUpdates(boolean checkForUpdates);

	void setBetas(boolean betas);

	String getAvailableVersion();

	String getInstalledVersion();

	boolean isUpdated();
}
