package org.argeo.jcr.fs;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Path;
import java.util.Map;

import javax.jcr.Session;

/** An FS provider based on a single JCR session (experimental). */
public class SessionFsProvider extends JcrFileSystemProvider {
	private Session session;
	private JcrFileSystem fileSystem;

	public SessionFsProvider(Session session) {
		this.session = session;
	}

	@Override
	public String getScheme() {
		return "jcr+session";
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		if (fileSystem != null && fileSystem.isOpen())
			throw new FileSystemAlreadyExistsException();
		fileSystem = new JcrFileSystem(this, session) {
			boolean open;

			@Override
			public void close() throws IOException {
				// prevent the session logout
				open = false;
			}

			@Override
			public boolean isOpen() {
				return open;
			}

		};
		return fileSystem;
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		return fileSystem;
	}

	@Override
	public Path getPath(URI uri) {
		return new JcrPath(fileSystem, uri.getPath());
	}

}
