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

import javax.jcr.Credentials;
import javax.jcr.Repository;
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

	private Credentials credentials;

	public JackrabbitMemoryFsProvider() {
		String username = System.getProperty("user.name");
		credentials = new SimpleCredentials(username, username.toCharArray());
	}

	@Override
	public String getScheme() {
		return "jcr+memory";
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		try {
			Path tempDir = Files.createTempDirectory("fs-memory");
			URL confUrl = JackrabbitMemoryFsProvider.class.getResource("fs-memory.xml");
			RepositoryConfig repositoryConfig = RepositoryConfig.create(confUrl.toURI(), tempDir.toString());
			repository = RepositoryImpl.create(repositoryConfig);
			postRepositoryCreation(repository);
			fileSystem = new JcrFileSystem(this, repository, credentials);
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

	public Repository getRepository() {
		return repository;
	}

	public Session login() throws RepositoryException {
		return getRepository().login(credentials);
	}

	/**
	 * Called after the repository has been created and before the file system is
	 * created.
	 */
	protected void postRepositoryCreation(RepositoryImpl repositoryImpl) throws RepositoryException {

	}
}
