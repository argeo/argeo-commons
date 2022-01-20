package org.argeo.cms.gcr.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.argeo.api.gcr.Content;
import org.argeo.api.gcr.ContentResourceException;
import org.argeo.api.gcr.spi.ContentProvider;
import org.argeo.api.gcr.spi.ProvidedSession;

public class FsContentProvider implements ContentProvider {
	private final Path rootPath;

	public FsContentProvider(Path rootPath) {
		super();
		this.rootPath = rootPath;
	}

	boolean isRoot(Path path) {
		try {
			return Files.isSameFile(rootPath, path);
		} catch (IOException e) {
			throw new ContentResourceException(e);
		}
	}

	@Override
	public Content get(ProvidedSession session, String mountPath, String relativePath) {
		return new FsContent(session, this, rootPath.resolve(relativePath));
	}
}
