package org.argeo.cms.fs;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;

import org.argeo.api.NodeConstants;
import org.argeo.jcr.Jcr;

/** Utilities around documents. */
public class CmsFsUtils {
	// TODO make it more robust and configurable
	private static String baseWorkspaceName = NodeConstants.SYS_WORKSPACE;

	public static Node getNode(Repository repository, Path path) {
		String workspaceName = path.getNameCount() == 0 ? baseWorkspaceName : path.getName(0).toString();
		String jcrPath = '/' + path.subpath(1, path.getNameCount()).toString();
		try {
			Session newSession;
			try {
				newSession = repository.login(workspaceName);
			} catch (NoSuchWorkspaceException e) {
				// base workspace
				newSession = repository.login(baseWorkspaceName);
				jcrPath = path.toString();
			}
			return newSession.getNode(jcrPath);
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get node from path " + path, e);
		}
	}

	public static NodeIterator getLastUpdatedDocuments(Session session) {
		try {
			String qStr = "//element(*, nt:file)";
			qStr += " order by @jcr:lastModified descending";
			QueryManager queryManager = session.getWorkspace().getQueryManager();
			@SuppressWarnings("deprecation")
			Query xpathQuery = queryManager.createQuery(qStr, Query.XPATH);
			xpathQuery.setLimit(8);
			NodeIterator nit = xpathQuery.execute().getNodes();
			return nit;
		} catch (RepositoryException e) {
			throw new IllegalStateException("Unable to retrieve last updated documents", e);
		}
	}

	public static Path getPath(FileSystemProvider nodeFileSystemProvider, URI uri) {
		try {
			FileSystem fileSystem = nodeFileSystemProvider.getFileSystem(uri);
			if (fileSystem == null)
				fileSystem = nodeFileSystemProvider.newFileSystem(uri, null);
			String path = uri.getPath();
			return fileSystem.getPath(path);
		} catch (IOException e) {
			throw new IllegalStateException("Unable to initialise file system for " + uri, e);
		}
	}

	public static Path getPath(FileSystemProvider nodeFileSystemProvider, Node node) {
		String workspaceName = Jcr.getWorkspaceName(node);
		String fullPath = baseWorkspaceName.equals(workspaceName) ? Jcr.getPath(node)
				: '/' + workspaceName + Jcr.getPath(node);
		URI uri;
		try {
			uri = new URI(NodeConstants.SCHEME_NODE, null, fullPath, null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("Cannot interpret " + fullPath + " as an URI", e);
		}
		return getPath(nodeFileSystemProvider, uri);
	}

	/** Singleton. */
	private CmsFsUtils() {
	}
}
