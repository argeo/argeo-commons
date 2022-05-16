package org.argeo.cms.acr.fs;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.util.concurrent.Callable;

import org.argeo.cms.acr.fs.SyncFileVisitor;
import org.argeo.cms.acr.fs.SyncResult;

/** Synchronises two paths. */
public class PathSync implements Callable<SyncResult<Path>> {
	private final URI sourceUri, targetUri;
	private final boolean delete;
	private final boolean recursive;

	public PathSync(URI sourceUri, URI targetUri) {
		this(sourceUri, targetUri, false, false);
	}

	public PathSync(URI sourceUri, URI targetUri, boolean delete, boolean recursive) {
		this.sourceUri = sourceUri;
		this.targetUri = targetUri;
		this.delete = delete;
		this.recursive = recursive;
	}

	@Override
	public SyncResult<Path> call() {
		try {
			Path sourceBasePath = createPath(sourceUri);
			Path targetBasePath = createPath(targetUri);
			SyncFileVisitor syncFileVisitor = new SyncFileVisitor(sourceBasePath, targetBasePath, delete, recursive);
			Files.walkFileTree(sourceBasePath, syncFileVisitor);
			return syncFileVisitor.getSyncResult();
		} catch (Exception e) {
			throw new IllegalStateException("Cannot sync " + sourceUri + " to " + targetUri, e);
		}
	}

	private Path createPath(URI uri) {
		Path path;
		if (uri.getScheme() == null) {
			path = Paths.get(uri.getPath());
		} else if (uri.getScheme().equals("file")) {
			FileSystemProvider fsProvider = FileSystems.getDefault().provider();
			path = fsProvider.getPath(uri);
		} else if (uri.getScheme().equals("davex")) {
			throw new UnsupportedOperationException();
//			FileSystemProvider fsProvider = new DavexFsProvider();
//			path = fsProvider.getPath(uri);
//		} else if (uri.getScheme().equals("sftp")) {
//			Sftp sftp = new Sftp(uri);
//			path = sftp.getBasePath();
		} else
			throw new IllegalArgumentException("URI scheme not supported for " + uri);
		return path;
	}
}
