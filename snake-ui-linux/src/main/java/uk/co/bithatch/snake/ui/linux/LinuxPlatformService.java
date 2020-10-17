package uk.co.bithatch.snake.ui.linux;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import uk.co.bithatch.snake.lib.Device;
import uk.co.bithatch.snake.ui.App;
import uk.co.bithatch.snake.ui.PlatformService;

public class LinuxPlatformService implements PlatformService {

	private static final String STABLE_CHANNEL = System.getProperty("forker.relaseChannel", "http://blue/repository");
	private static final String BETA_CHANNEL = System.getProperty("forker.betaChannel", "http://blue/beta");

	private static final String SNAKE_RAZER_DESKTOP = "snake-razer.desktop";

	private void writeDesktopFile(File file, String name, String comment, Boolean autoStart, String... execArgs)
			throws IOException, FileNotFoundException {
		checkFilesParent(file);
		try (PrintWriter pw = new PrintWriter(new FileWriter(file))) {
			pw.println("#!/usr/bin/env xdg-open");
			pw.println("[Desktop Entry]");
			pw.println("Version=1.0");
			pw.println("Terminal=false");
			File iconFile = checkFilesParent(new File(getShare(), "pixmaps" + File.separator + "snake-razer.png"));
			try (FileOutputStream fos = new FileOutputStream(iconFile)) {
				try (InputStream in = App.class.getResourceAsStream("appicon/razer-color-512.png")) {
					in.transferTo(fos);
				}
			}
			pw.println("Icon=" + iconFile.getAbsolutePath());
			pw.println("Exec=" + System.getProperty("user.dir") + File.separator + "bin/snake"
					+ (execArgs.length == 0 ? "" : " " + String.join(" ", execArgs)));
			pw.println("Name=" + name);
			pw.println("Comment=" + comment);
			pw.println("Categories=Utility;Core;");
			pw.println("StartupNotify=false");
			pw.println("Type=Application");
			pw.println("Keywords=razer;snake;mamba;chroma;deathadder");
			if (autoStart != null) {
				pw.println("X-GNOME-Autostart-enabled=" + autoStart);
			}
		}
	}

	private File checkFilesParent(File file) throws IOException {
		if (!file.exists() && !file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
			throw new IOException(String.format("Failed to create parent folder for %s.", file));
		}
		return file;
	}

	File getShare() {
		return new File(System.getProperty("user.home") + File.separator + ".local" + File.separator + "share");
	}

	File getShortcutFile() {
		return new File(getShare(), "applications" + File.separator + SNAKE_RAZER_DESKTOP);
	}

	File getAutostartFile() {
		return new File(System.getProperty("user.home") + File.separator + ".config" + File.separator + "autostart"
				+ File.separator + SNAKE_RAZER_DESKTOP);
	}

	@Override
	public boolean isStartOnLogin() {
		File f = getAutostartFile();
		if (!f.exists())
			return false;
		try (BufferedReader r = new BufferedReader(new FileReader(f))) {
			String line;
			while ((line = r.readLine()) != null) {
				line = line.trim();
				if (line.startsWith("X-GNOME-Autostart-enabled=")) {
					return line.substring(26).equals("true");
				}
			}
		} catch (IOException ioe) {
		}
		return false;
	}

	@Override
	public void setStartOnLogin(boolean startOnLogin) throws IOException {
		writeDesktopFile(getAutostartFile(), "Snake", "Control and configure your Razer devices", startOnLogin,
				"-- --no-open");
	}

	@Override
	public boolean isUpdateableApp() {
		return Files.exists(Paths.get("manifest.xml")) || isDev();
	}

	@Override
	public boolean isUpdateAvailable() {
		return "true".equals(System.getProperty("forker.updateAvailable"));
	}

	@Override
	public boolean isUpdateAutomatically() {
		try {
			Path path = checkDir(getAppCfg()).resolve("updates");
			return !Files.exists(path) || !contains(path, "update-on-exit") || contains(path, "update-on-exit 99");
		} catch (IOException ioe) {
			return false;
		}
	}

	@Override
	public boolean isCheckForUpdates() {
		try {
			Path path = checkDir(getAppCfg()).resolve("updates");
			return !Files.exists(path) || !contains(path, "update-on-exit") || contains(path, "update-on-exit 100");
		} catch (IOException ioe) {
			return false;
		}
	}

	@Override
	public boolean isBetas() {
		try {
			Path path = checkDir(getAppCfg()).resolve("updates");
			return Files.exists(path) && contains(path, "remote-manifest " + BETA_CHANNEL);
		} catch (IOException ioe) {
			return false;
		}
	}

	@Override
	public void setUpdateAutomatically(boolean updateAutomatically) {
		reconfig(isCheckForUpdates(), updateAutomatically, isBetas());
	}

	@Override
	public void setCheckForUpdates(boolean checkForUpdates) {
		reconfig(checkForUpdates, isUpdateAutomatically(), isBetas());
	}

	@Override
	public void setBetas(boolean betas) {
		reconfig(isCheckForUpdates(), isUpdateAutomatically(), betas);
	}

	protected void reconfig(boolean check, boolean auto, boolean betas) {
		try {
			Path path = checkDir(getAppCfg()).resolve("updates");
			try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(path)), true)) {
				if (check) {
					if (!auto)
						pw.println("update-on-exit 100");
				} else {
					pw.println("update-on-exit 99");
				}
				if (betas)
					pw.println("remote-manifest " + BETA_CHANNEL);
				else
					pw.println("remote-manifest " + STABLE_CHANNEL);
			}
		} catch (IOException ioe) {
			throw new IllegalStateException("Failed to change update state.", ioe);
		}
	}

	protected boolean contains(Path path, String str) throws IOException {
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(Files.newInputStream(path)))) {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().startsWith(str)) {
					return true;
				}
			}
		}
		return false;
	}

	protected Path checkDir(Path dir) throws IOException {
		if (!Files.exists(dir)) {
			Files.createDirectories(dir);
		}
		return dir;
	}

	protected Path getAppCfg() {
		if (isDev())
			return Paths.get("target/image/app.cfg.d");
		else
			return Paths.get("app.cfg.d");
	}

	protected boolean isDev() {
		return Files.exists(Paths.get("pom.xml"));
	}

	@Override
	public String getAvailableVersion() {
		return System.getProperty("forker.availableVersion", getInstalledVersion());
	}

	@Override
	public String getInstalledVersion() {
		return System.getProperty("forker.installedVersion", "Unknown");
	}

	@Override
	public boolean isUpdated() {
		return "true".equals(System.getProperty("forker.updated"));
	}
}
