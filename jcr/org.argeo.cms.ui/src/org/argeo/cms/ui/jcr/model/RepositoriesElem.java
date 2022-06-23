package org.argeo.cms.ui.jcr.model;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;

import org.argeo.cms.ArgeoNames;
import org.argeo.cms.jcr.CmsJcrUtils;
import org.argeo.cms.security.Keyring;
import org.argeo.cms.ui.jcr.RepositoryRegister;
import org.argeo.cms.ux.widgets.TreeParent;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;

/**
 * UI Tree component that implements the Argeo abstraction of a
 * {@link RepositoryFactory} that enable a user to "mount" various repositories
 * in a single Tree like View. It is usually meant to be at the root of the UI
 * Tree and thus {@link getParent()} method will return null.
 * 
 * The {@link RepositoryFactory} is injected at instantiation time and must be
 * use get or register new {@link Repository} objects upon which a reference is
 * kept here.
 */

public class RepositoriesElem extends TreeParent implements ArgeoNames {
	private final RepositoryRegister repositoryRegister;
	private final RepositoryFactory repositoryFactory;

	/**
	 * A session of the logged in user on the default workspace of the node
	 * repository.
	 */
	private final Session userSession;
	private final Keyring keyring;

	public RepositoriesElem(String name, RepositoryRegister repositoryRegister, RepositoryFactory repositoryFactory,
			TreeParent parent, Session userSession, Keyring keyring) {
		super(name);
		this.repositoryRegister = repositoryRegister;
		this.repositoryFactory = repositoryFactory;
		this.userSession = userSession;
		this.keyring = keyring;
	}

	/**
	 * Override normal behavior to initialize the various repositories only at
	 * request time
	 */
	@Override
	public synchronized Object[] getChildren() {
		if (isLoaded()) {
			return super.getChildren();
		} else {
			// initialize current object
			Map<String, Repository> refRepos = repositoryRegister.getRepositories();
			for (String name : refRepos.keySet()) {
				Repository repository = refRepos.get(name);
				// if (repository instanceof MaintainedRepository)
				// super.addChild(new MaintainedRepositoryElem(name,
				// repository, this));
				// else
				super.addChild(new RepositoryElem(name, repository, this));
			}

			// remote
			if (keyring != null) {
				try {
					addRemoteRepositories(keyring);
				} catch (RepositoryException e) {
					throw new EclipseUiException("Cannot browse remote repositories", e);
				}
			}
			return super.getChildren();
		}
	}

	protected void addRemoteRepositories(Keyring jcrKeyring) throws RepositoryException {
		Node userHome = CmsJcrUtils.getUserHome(userSession);
		if (userHome != null && userHome.hasNode(ARGEO_REMOTE)) {
			NodeIterator it = userHome.getNode(ARGEO_REMOTE).getNodes();
			while (it.hasNext()) {
				Node remoteNode = it.nextNode();
				String uri = remoteNode.getProperty(ARGEO_URI).getString();
				try {
					RemoteRepositoryElem remoteRepositoryNode = new RemoteRepositoryElem(remoteNode.getName(),
							repositoryFactory, uri, this, userSession, jcrKeyring, remoteNode.getPath());
					super.addChild(remoteRepositoryNode);
				} catch (Exception e) {
					ErrorFeedback.show("Cannot add remote repository " + remoteNode, e);
				}
			}
		}
	}

	public void registerNewRepository(String alias, Repository repository) {
		// TODO: implement this
		// Create a new RepositoryNode Object
		// add it
		// super.addChild(new RepositoriesNode(...));
	}

	/** Returns the {@link RepositoryRegister} wrapped by this object. */
	public RepositoryRegister getRepositoryRegister() {
		return repositoryRegister;
	}
}
