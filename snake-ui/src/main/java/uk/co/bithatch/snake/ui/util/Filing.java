package uk.co.bithatch.snake.ui.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Filing {
	public static void unzip(Path zip, Path destDir) throws IOException {
		byte[] buffer = new byte[1024];
		try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
			ZipEntry zipEntry = zis.getNextEntry();
			while (zipEntry != null) {
				Files.createDirectories(destDir);
				Path newFile = newFile(destDir, zipEntry);
				Files.createDirectories(destDir.getParent());
				try (OutputStream fos = Files.newOutputStream(newFile)) {
					int len;
					while ((len = zis.read(buffer)) > 0) {
						fos.write(buffer, 0, len);
					}
				}
				zipEntry = zis.getNextEntry();
			}
			zis.closeEntry();
		}
	}

	public static Path newFile(Path destinationDir, ZipEntry zipEntry) throws IOException {
		Path destFile = destinationDir.resolve(zipEntry.getName());

		String destDirPath = destinationDir.toRealPath().toString();
		Files.createDirectories(destFile.getParent());
		String destFilePath = destFile.getParent().toRealPath().resolve(destFile.getFileName()).toString();

		if (!destFilePath.startsWith(destDirPath + File.separator)) {
			throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
		}

		return destFile;
	}

	/**
	 * Recursively deletes `item`, which may be a directory. Symbolic links will be
	 * deleted instead of their referents. Returns a boolean indicating whether
	 * `item` still exists. http://stackoverflow.com/questions/8666420
	 * 
	 * @param item file to delete
	 * @return deleted OK
	 */
	public static boolean deleteRecursiveIfExists(File item) {
		if (!item.exists())
			return true;
		if (!Files.isSymbolicLink(item.toPath()) && item.isDirectory()) {
			File[] subitems = item.listFiles();
			for (File subitem : subitems)
				if (!deleteRecursiveIfExists(subitem))
					return false;
		}
		return item.delete();
	}
}
