package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import org.argeo.api.NodeConstants;
import org.argeo.api.NodeUtils;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.jackrabbit.fs.AbstractJackrabbitFsProvider;
import org.argeo.jcr.fs.JcrFileSystem;
import org.argeo.jcr.fs.JcrFileSystemProvider;
import org.argeo.jcr.fs.JcrFsException;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class CmsFsProvider extends AbstractJackrabbitFsProvider {
	private Map<String, CmsFileSystem> fileSystems = new HashMap<>();

	@Override
	public String getScheme() {
		return NodeConstants.SCHEME_NODE;
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		BundleContext bc = FrameworkUtil.getBundle(CmsFsProvider.class).getBundleContext();
		String username = CurrentUser.getUsername();
		if (username == null) {
			// TODO deal with anonymous
			return null;
		}
		if (fileSystems.containsKey(username))
			throw new FileSystemAlreadyExistsException("CMS file system already exists for user " + username);

		try {
			String host = uri.getHost();
			if (host != null && !host.trim().equals("")) {
				URI repoUri = new URI("http", uri.getUserInfo(), uri.getHost(), uri.getPort(), "/jcr/node", null, null);
				RepositoryFactory repositoryFactory = bc.getService(bc.getServiceReference(RepositoryFactory.class));
				Repository repository = NodeUtils.getRepositoryByUri(repositoryFactory, repoUri.toString());
//				Session session = repository.login("main");
				CmsFileSystem fileSystem = new CmsFileSystem(this, repository);
				fileSystems.put(username, fileSystem);
				return fileSystem;
			} else {
				Repository repository = bc.getService(
						bc.getServiceReferences(Repository.class, "(cn=" + NodeConstants.EGO + ")").iterator().next());
//				Session session = repository.login();
				CmsFileSystem fileSystem = new CmsFileSystem(this, repository);
				fileSystems.put(username, fileSystem);
				return fileSystem;
			}
		} catch (Exception e) {
			throw new CmsException("Cannot open file system " + uri + " for user " + username, e);
		}
	}

	@Override
	public FileSystem getFileSystem(URI uri) {
		return currentUserFileSystem();
	}

	@Override
	public Path getPath(URI uri) {
		JcrFileSystem fileSystem = currentUserFileSystem();
		String path = uri.getPath();
		if (fileSystem == null)
			try {
				fileSystem = (JcrFileSystem) newFileSystem(uri, new HashMap<String, Object>());
			} catch (IOException e) {
				throw new JcrFsException("Could not autocreate file system", e);
			}
		return fileSystem.getPath(path);
	}

	protected JcrFileSystem currentUserFileSystem() {
		String username = CurrentUser.getUsername();
		return fileSystems.get(username);
	}

	public Node getUserHome(Repository repository) {
		try {
			Session session = repository.login(NodeConstants.HOME);
			return NodeUtils.getUserHome(session);
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get user home", e);
		}
	}

	static class CmsFileSystem extends JcrFileSystem {
		public CmsFileSystem(JcrFileSystemProvider provider, Repository repository) throws IOException {
			super(provider, repository);
		}

		public boolean skipNode(Node node) throws RepositoryException {
//			if (node.isNodeType(NodeType.NT_HIERARCHY_NODE) || node.isNodeType(NodeTypes.NODE_USER_HOME)
//					|| node.isNodeType(NodeTypes.NODE_GROUP_HOME))
			if (node.isNodeType(NodeType.NT_HIERARCHY_NODE))
				return false;
			// FIXME Better identifies home
			if (node.hasProperty(Property.JCR_ID))
				return false;
			return true;
		}

	}
}
