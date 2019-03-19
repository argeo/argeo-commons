package org.argeo.ssh;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;

import org.apache.sshd.client.subsystem.sftp.fs.SftpFileSystem;

public class Sftp extends AbstractSsh {
	private URI uri;

	private SftpFileSystem fileSystem;

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
