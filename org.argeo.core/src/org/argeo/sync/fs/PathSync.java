package org.argeo.sync.fs;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;

import org.argeo.jackrabbit.fs.DavexFsProvider;
import org.argeo.ssh.Sftp;
import org.argeo.sync.SyncException;

/** Synchronises two paths. */
public class PathSync implements Runnable {
	private final URI sourceUri, targetUri;
	private boolean delete = false;

	public PathSync(URI sourceUri, URI targetUri) {
		this.sourceUri = sourceUri;
		this.targetUri = targetUri;
	}

	@Override
	public void run() {
		try {
			Path sourceBasePath = createPath(sourceUri);
			Path targetBasePath = createPath(targetUri);
			SyncFileVisitor syncFileVisitor = new SyncFileVisitor(sourceBasePath, targetBasePath, delete);
			Files.walkFileTree(sourceBasePath, syncFileVisitor);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static Path createPath(URI uri) {
		Path path;
		if (uri.getScheme() == null) {
			path = Paths.get(uri.getPath());
		} else if (uri.getScheme().equals("file")) {
			FileSystemProvider fsProvider = FileSystems.getDefault().provider();
			path = fsProvider.getPath(uri);
		} else if (uri.getScheme().equals("davex")) {
			FileSystemProvider fsProvider = new DavexFsProvider();
			path = fsProvider.getPath(uri);
		} else if (uri.getScheme().equals("sftp")) {
			Sftp sftp = new Sftp(uri);
			path = sftp.getBasePath();
		} else
			throw new SyncException("URI scheme not supported for " + uri);
		return path;
	}
}
