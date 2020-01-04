package org.argeo.fs;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;

/** Synchronises two directory structures. */
public class BasicSyncFileVisitor extends SimpleFileVisitor<Path> {
	// TODO make it configurable
	private boolean debug = false;

	private final Path sourceBasePath;
	private final Path targetBasePath;
	private final boolean delete;

	public BasicSyncFileVisitor(Path sourceBasePath, Path targetBasePath, boolean delete) {
		this.sourceBasePath = sourceBasePath;
		this.targetBasePath = targetBasePath;
		this.delete = delete;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path sourceDir, BasicFileAttributes attrs) throws IOException {
		Path targetDir = toTargetPath(sourceDir);
		Files.createDirectories(targetDir);
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult postVisitDirectory(Path sourceDir, IOException exc) throws IOException {
		if (delete) {
			Path targetDir = toTargetPath(sourceDir);
			for (Path targetPath : Files.newDirectoryStream(targetDir)) {
				Path sourcePath = sourceDir.resolve(targetPath.getFileName());
				if (!Files.exists(sourcePath)) {
					try {
						FsUtils.delete(targetPath);
						deleted(targetPath);
					} catch (Exception e) {
						deleteFailed(targetPath, exc);
					}
				}
			}
		}
		return FileVisitResult.CONTINUE;
	}

	@Override
	public FileVisitResult visitFile(Path sourceFile, BasicFileAttributes attrs) throws IOException {
		Path targetFile = toTargetPath(sourceFile);
		try {
			if (!Files.exists(targetFile)) {
				Files.copy(sourceFile, targetFile);
				copied(sourceFile, targetFile);
			} else {
				if (shouldOverwrite(sourceFile, targetFile)) {
					Files.copy(sourceFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
				}
			}
		} catch (Exception e) {
			copyFailed(sourceFile, targetFile, e);
		}
		return FileVisitResult.CONTINUE;
	}

	protected boolean shouldOverwrite(Path sourceFile, Path targetFile) throws IOException {
		long sourceSize = Files.size(sourceFile);
		long targetSize = Files.size(targetFile);
		if (sourceSize != targetSize) {
			return true;
		}
		FileTime sourceLastModif = Files.getLastModifiedTime(sourceFile);
		FileTime targetLastModif = Files.getLastModifiedTime(targetFile);
		if (sourceLastModif.compareTo(targetLastModif) > 0)
			return true;
		return shouldOverwriteLaterSameSize(sourceFile, targetFile);
	}

	protected boolean shouldOverwriteLaterSameSize(Path sourceFile, Path targetFile) {
		return false;
	}

//	@Override
//	public FileVisitResult visitFileFailed(Path sourceFile, IOException exc) throws IOException {
//		error("Cannot sync " + sourceFile, exc);
//		return FileVisitResult.CONTINUE;
//	}

	private Path toTargetPath(Path sourcePath) {
		Path relativePath = sourceBasePath.relativize(sourcePath);
		Path targetPath = targetBasePath.resolve(relativePath.toString());
		return targetPath;
	}

	public Path getSourceBasePath() {
		return sourceBasePath;
	}

	public Path getTargetBasePath() {
		return targetBasePath;
	}

	protected void copied(Path sourcePath, Path targetPath) {
		if (isDebugEnabled())
			debug("Copied " + sourcePath + " to " + targetPath);
	}

	protected void copyFailed(Path sourcePath, Path targetPath, Exception e) {
		error("Cannot copy " + sourcePath + " to " + targetPath, e);
	}

	protected void deleted(Path targetPath) {
		if (isDebugEnabled())
			debug("Deleted " + targetPath);
	}

	protected void deleteFailed(Path targetPath, Exception e) {
		error("Cannot delete " + targetPath, e);
	}

	/** Log error. */
	protected void error(Object obj, Throwable e) {
		System.err.println(obj);
		e.printStackTrace();
	}

	protected boolean isDebugEnabled() {
		return debug;
	}

	protected void debug(Object obj) {
		System.out.println(obj);
	}
}
