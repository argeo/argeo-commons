package org.argeo.cms.acr.fs;

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
	private boolean trace = false;

	private final Path sourceBasePath;
	private final Path targetBasePath;
	private final boolean delete;
	private final boolean recursive;

	private SyncResult<Path> syncResult = new SyncResult<>();

	public BasicSyncFileVisitor(Path sourceBasePath, Path targetBasePath, boolean delete, boolean recursive) {
		this.sourceBasePath = sourceBasePath;
		this.targetBasePath = targetBasePath;
		this.delete = delete;
		this.recursive = recursive;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path sourceDir, BasicFileAttributes attrs) throws IOException {
		if (!recursive && !sourceDir.equals(sourceBasePath))
			return FileVisitResult.SKIP_SUBTREE;
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
						FsSyncUtils.delete(targetPath);
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
				added(sourceFile, targetFile);
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

	protected void added(Path sourcePath, Path targetPath) {
		syncResult.getAdded().add(targetPath);
		if (isTraceEnabled())
			trace("Added " + sourcePath + " as " + targetPath);
	}

	protected void modified(Path sourcePath, Path targetPath) {
		syncResult.getModified().add(targetPath);
		if (isTraceEnabled())
			trace("Overwritten from " + sourcePath + " to " + targetPath);
	}

	protected void copyFailed(Path sourcePath, Path targetPath, Exception e) {
		syncResult.addError(sourcePath, targetPath, e);
		if (isTraceEnabled())
			error("Cannot copy " + sourcePath + " to " + targetPath, e);
	}

	protected void deleted(Path targetPath) {
		syncResult.getDeleted().add(targetPath);
		if (isTraceEnabled())
			trace("Deleted " + targetPath);
	}

	protected void deleteFailed(Path targetPath, Exception e) {
		syncResult.addError(null, targetPath, e);
		if (isTraceEnabled())
			error("Cannot delete " + targetPath, e);
	}

	/** Log error. */
	protected void error(Object obj, Throwable e) {
		System.err.println(obj);
		e.printStackTrace();
	}

	protected boolean isTraceEnabled() {
		return trace;
	}

	protected void trace(Object obj) {
		System.out.println(obj);
	}

	public SyncResult<Path> getSyncResult() {
		return syncResult;
	}

}
