package org.argeo.ssh;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.apache.sshd.client.subsystem.sftp.fs.SftpFileSystem;

/** Create an SFTP {@link FileSystem}. */
public class Sftp extends AbstractSsh {
	private URI uri;

	private SftpFileSystem fileSystem;

	public Sftp(String username, String host, int port) {
		this(AbstractSsh.toUri(username, host, port));
	}

	public Sftp(URI uri) {
		this.uri = uri;
		openSession(uri);
	}

	public FileSystem getFileSystem() {
		if (fileSystem == null) {
			try {
				authenticate();
				fileSystem = getSftpFileSystemProvider().newFileSystem(getSession());
			} catch (IOException e) {
				throw new IllegalStateException(e);
			}
		}
		return fileSystem;
	}

	public Path getBasePath() {
		String p = uri.getPath() != null ? uri.getPath() : "/";
		return getFileSystem().getPath(p);
	}

}
