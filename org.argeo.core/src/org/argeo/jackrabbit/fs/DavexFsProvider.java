package org.argeo.jackrabbit.fs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;

import org.argeo.jackrabbit.client.ClientDavexRepositoryFactory;
import org.argeo.jcr.fs.JcrFileSystem;
import org.argeo.jcr.fs.JcrFsException;

/**
 * A file system provider based on a JCR repository remotely accessed via the
 * DAVEX protocol.
 */
public class DavexFsProvider extends AbstractJackrabbitFsProvider {
//	final static String JACKRABBIT_REPOSITORY_URI = "org.apache.jackrabbit.repository.uri";
//	final static String JACKRABBIT_REMOTE_DEFAULT_WORKSPACE = "org.apache.jackrabbit.spi2davex.WorkspaceNameDefault";

	private Map<String, JcrFileSystem> fileSystems = new HashMap<>();

	@Override
	public String getScheme() {
		return "davex";
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		if (uri.getHost() == null)
			throw new IllegalArgumentException("An host should be provided");
		try {
			URI repoUri = new URI("http", uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), null, null);
			String repoKey = repoUri.toString();
			if (fileSystems.containsKey(repoKey))
				throw new FileSystemAlreadyExistsException("CMS file system already exists for " + repoKey);
			RepositoryFactory repositoryFactory = new ClientDavexRepositoryFactory();
			return tryGetRepo(repositoryFactory, repoUri, "home");
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Cannot open file system " + uri, e);
		}
	}

	private JcrFileSystem tryGetRepo(RepositoryFactory repositoryFactory, URI repoUri, String workspace)
			throws IOException {
		Map<String, String> params = new HashMap<String, String>();
		params.put(ClientDavexRepositoryFactory.JACKRABBIT_DAVEX_URI, repoUri.toString());
		// FIXME make it configurable
		params.put(ClientDavexRepositoryFactory.JACKRABBIT_REMOTE_DEFAULT_WORKSPACE, "sys");
		Repository repository = null;
		Session session = null;
		try {
			repository = repositoryFactory.getRepository(params);
			if (repository != null)
				session = repository.login(workspace);
		} catch (Exception e) {
			// silent
		}

		if (session == null) {
			if (repoUri.getPath() == null || repoUri.getPath().equals("/"))
				return null;
			String repoUriStr = repoUri.toString();
			if (repoUriStr.endsWith("/"))
				repoUriStr = repoUriStr.substring(0, repoUriStr.length() - 1);
			String nextRepoUriStr = repoUriStr.substring(0, repoUriStr.lastIndexOf('/'));
			String nextWorkspace = repoUriStr.substring(repoUriStr.lastIndexOf('/') + 1);
			URI nextUri;
			try {
				nextUri = new URI(nextRepoUriStr);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException("Badly formatted URI", e);
			}
			return tryGetRepo(repositoryFactory, nextUri, nextWorkspace);
		} else {
			JcrFileSystem fileSystem = new JcrFileSystem(this, repository);
			fileSystems.put(repoUri.toString() + "/" + workspace, fileSystem);
			return fileSystem;
		}
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		return currentUserFileSystem(uri);
	}

	@Override
	public Path getPath(URI uri) {
		JcrFileSystem fileSystem = currentUserFileSystem(uri);
		if (fileSystem == null)
			try {
				fileSystem = (JcrFileSystem) newFileSystem(uri, new HashMap<String, Object>());
			} catch (IOException e) {
				throw new JcrFsException("Could not autocreate file system", e);
			}
		URI repoUri = null;
		try {
			repoUri = new URI("http", uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), null, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e);
		}
		String uriStr = repoUri.toString();
		String localPath = null;
		for (String key : fileSystems.keySet()) {
			if (uriStr.startsWith(key)) {
				localPath = uriStr.toString().substring(key.length());
			}
		}
		if ("".equals(localPath))
			localPath = "/";
		return fileSystem.getPath(localPath);
	}

	private JcrFileSystem currentUserFileSystem(URI uri) {
		for (String key : fileSystems.keySet()) {
			if (uri.toString().startsWith(key))
				return fileSystems.get(key);
		}
		return null;
	}

	public static void main(String args[]) {
		try {
			DavexFsProvider fsProvider = new DavexFsProvider();
			Path path = fsProvider.getPath(new URI("davex://root:demo@localhost:7070/jcr/ego/"));
			System.out.println(path);
			DirectoryStream<Path> ds = Files.newDirectoryStream(path);
			for (Path p : ds) {
				System.out.println("- " + p);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
