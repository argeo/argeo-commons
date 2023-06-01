package org.argeo.cms.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/** Utilities around the standard Java file abstractions. */
public class FsUtils {

	/** Deletes this path, recursively if needed. */
	public static void copyDirectory(Path source, Path target) {
		if (!Files.exists(source) || !Files.isDirectory(source))
			throw new IllegalArgumentException(source + " is not a directory");
		if (Files.exists(target) && !Files.isDirectory(target))
			throw new IllegalArgumentException(target + " is not a directory");
		try {
			Files.createDirectories(target);
			Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attrs) throws IOException {
					Path relativePath = source.relativize(directory);
					Path targetDirectory = target.resolve(relativePath);
					if (!Files.exists(targetDirectory))
						Files.createDirectory(targetDirectory);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Path relativePath = source.relativize(file);
					Path targetFile = target.resolve(relativePath);
					Files.copy(file, targetFile);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			throw new RuntimeException("Cannot copy " + source + " to " + target, e);
		}

	}

	/**
	 * Deletes this path, recursively if needed. Does nothing if the path does not
	 * exist.
	 */
	public static void delete(Path path) throws IOException {
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
	}

	/** Singleton. */
	private FsUtils() {
	}

}
