package org.argeo.cms.cmd;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/** Synchronises two directory structures. */
public class SyncFileVisitor extends SimpleFileVisitor<Path> {
	private final static Log log = LogFactory.getLog(SyncFileVisitor.class);

	private final Path sourceBasePath;
	private final Path targetBasePath;

	public SyncFileVisitor(Path sourceBasePath, Path targetBasePath) {
		this.sourceBasePath = sourceBasePath;
		this.targetBasePath = targetBasePath;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
		Path targetPath = toTargetPath(dir);
		Files.createDirectories(targetPath);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
		Path targetPath = toTargetPath(file);
		try {
			Files.copy(file, targetPath);
			if (log.isDebugEnabled())
				log.debug("Copied " + targetPath);
		} catch (Exception e) {
			log.error("Cannot copy " + file + " to " + targetPath, e);
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		log.error("Cannot sync " + file, exc);
		return FileVisitResult.CONTINUE;
	}

	private Path toTargetPath(Path sourcePath) {
		Path relativePath = sourceBasePath.relativize(sourcePath);
		Path targetPath = targetBasePath.resolve(relativePath.toString());
		return targetPath;
	}
}
