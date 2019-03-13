package org.argeo.sync.fs;

import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.spi.FileSystemProvider;
import java.time.ZonedDateTime;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.jackrabbit.fs.DavexFsProvider;
import org.argeo.ssh.Sftp;
import org.argeo.sync.SyncException;
import org.argeo.util.LangUtils;

public class PathSync implements Runnable {
	private final static Log log = LogFactory.getLog(PathSync.class);

	private final URI sourceUri, targetUri;

	public PathSync(URI sourceUri, URI targetUri) {
		this.sourceUri = sourceUri;
		this.targetUri = targetUri;
	}

	@Override
	public void run() {
		try {
			Path sourceBasePath = createPath(sourceUri);
			Path targetBasePath = createPath(targetUri);
			SyncFileVisitor syncFileVisitor = new SyncFileVisitor(sourceBasePath, targetBasePath);
			ZonedDateTime begin = ZonedDateTime.now();
			Files.walkFileTree(sourceBasePath, syncFileVisitor);
			if (log.isDebugEnabled())
				log.debug("Sync from " + sourceBasePath + " to " + targetBasePath + " took " + LangUtils.since(begin));
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

	static enum Arg {
		to, from;
	}
}
