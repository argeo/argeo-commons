package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.Session;

import org.argeo.cms.CmsException;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.jackrabbit.fs.AbstractJackrabbitFsProvider;
import org.argeo.jcr.fs.JcrFileSystem;
import org.argeo.jcr.fs.JcrFsException;
import org.argeo.node.NodeConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

public class CmsFsProvider extends AbstractJackrabbitFsProvider {
	private Map<String, JcrFileSystem> fileSystems = new HashMap<>();
	private BundleContext bc = FrameworkUtil.getBundle(CmsFsProvider.class).getBundleContext();

	@Override
	public String getScheme() {
		return NodeConstants.SCHEME_NODE;
	}

	@Override
	public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
		String username = CurrentUser.getUsername();
		if (username == null) {
			// TODO deal with anonymous
			return null;
		}
		if (fileSystems.containsKey(username))
			throw new FileSystemAlreadyExistsException("CMS file system already exists for user " + username);

		try {
			Repository repository = bc.getService(
					bc.getServiceReferences(Repository.class, "(cn=" + NodeConstants.HOME + ")").iterator().next());
			Session session = repository.login();
			JcrFileSystem fileSystem = new JcrFileSystem(this, session);
			fileSystems.put(username, fileSystem);
			return fileSystem;
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
}
