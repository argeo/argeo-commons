package org.argeo.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/** Utilities around the standard Java file abstractions. */
public class FsUtils {
	/**
	 * Deletes this path, recursively if needed. Does nothing if the path does not
	 * exist.
	 */
	public static void delete(Path path) {
		try {
			if (!Files.exists(path))
				return;
			Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult postVisitDirectory(Path directory, IOException e) throws IOException {
					if (e != null)
						throw e;
					Files.delete(directory);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("Cannot delete " + path, e);
		}
	}

	/** Singleton. */
	private FsUtils() {
	}

}
