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
			FileSystemProvider sourceFsProvider = createFsProvider(sourceUri);
			FileSystemProvider targetFsProvider = createFsProvider(targetUri);
			Path sourceBasePath = sourceUri.getScheme() != null ? sourceFsProvider.getPath(sourceUri)
					: Paths.get(sourceUri.getPath());
			Path targetBasePath = targetUri.getScheme() != null ? targetFsProvider.getPath(targetUri)
					: Paths.get(targetUri.getPath());
			SyncFileVisitor syncFileVisitor = new SyncFileVisitor(sourceBasePath, targetBasePath);
			ZonedDateTime begin = ZonedDateTime.now();
			Files.walkFileTree(sourceBasePath, syncFileVisitor);
			if (log.isDebugEnabled())
				log.debug("Sync from " + sourceBasePath + " to " + targetBasePath + " took " + LangUtils.since(begin));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static FileSystemProvider createFsProvider(URI uri) {
		FileSystemProvider fsProvider;
		if (uri.getScheme() == null || uri.getScheme().equals("file"))
			fsProvider = FileSystems.getDefault().provider();
		else if (uri.getScheme().equals("davex"))
			fsProvider = new DavexFsProvider();
		else
			throw new SyncException("URI scheme not supported for " + uri);
		return fsProvider;
	}

	static enum Arg {
		to, from;
	}
}
