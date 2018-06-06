package org.argeo.jackrabbit.fs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.argeo.jcr.fs.JcrFileSystem;
import org.argeo.jcr.fs.JcrFsException;

public class JackrabbitMemoryFsProvider extends AbstractJackrabbitFsProvider {
	private RepositoryImpl repository;
	private JcrFileSystem fileSystem;

	@Override
	public String getScheme() {
		return "jcr+memory";
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		try {
			Path tempDir = Files.createTempDirectory("fs-memory");
			URL confUrl = getClass().getResource("fs-memory.xml");
			RepositoryConfig repositoryConfig = RepositoryConfig.create(confUrl.toURI(), tempDir.toString());
			repository = RepositoryImpl.create(repositoryConfig);
			String username = System.getProperty("user.name");
			Session session = repository.login(new SimpleCredentials(username, username.toCharArray()));
			fileSystem = new JcrFileSystem(this, session);
			return fileSystem;
		} catch (RepositoryException | URISyntaxException e) {
			throw new IOException("Cannot login to repository", e);
		}
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		return fileSystem;
	}

	@Override
	public Path getPath(URI uri) {
		String path = uri.getPath();
		if (fileSystem == null)
			try {
				newFileSystem(uri, new HashMap<String, Object>());
			} catch (IOException e) {
				throw new JcrFsException("Could not autocreate file system", e);
			}
		return fileSystem.getPath(path);
	}

}
