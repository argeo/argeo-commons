package org.argeo.cms.ui.jcr.model;

import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.argeo.cms.ArgeoNames;
import org.argeo.cms.jcr.CmsJcrUtils;
import org.argeo.cms.security.Keyring;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.TreeParent;

/** Root of a remote repository */
public class RemoteRepositoryElem extends RepositoryElem {
	private final Keyring keyring;
	/**
	 * A session of the logged in user on the default workspace of the node
	 * repository.
	 */
	private final Session userSession;
	private final String remoteNodePath;

	private final RepositoryFactory repositoryFactory;
	private final String uri;

	public RemoteRepositoryElem(String alias, RepositoryFactory repositoryFactory, String uri, TreeParent parent,
			Session userSession, Keyring keyring, String remoteNodePath) {
		super(alias, null, parent);
		this.repositoryFactory = repositoryFactory;
		this.uri = uri;
		this.keyring = keyring;
		this.userSession = userSession;
		this.remoteNodePath = remoteNodePath;
	}

	@Override
	protected Session repositoryLogin(String workspaceName) throws RepositoryException {
		Node remoteRepository = userSession.getNode(remoteNodePath);
		String userID = remoteRepository.getProperty(ArgeoNames.ARGEO_USER_ID).getString();
		if (userID.trim().equals("")) {
			return getRepository().login(workspaceName);
		} else {
			String pwdPath = remoteRepository.getPath() + '/' + ArgeoNames.ARGEO_PASSWORD;
			char[] password = keyring.getAsChars(pwdPath);
			try {
				SimpleCredentials credentials = new SimpleCredentials(userID, password);
				return getRepository().login(credentials, workspaceName);
			} finally {
				Arrays.fill(password, 0, password.length, ' ');
			}
		}
	}

	@Override
	public Repository getRepository() {
		if (repository == null)
			repository = CmsJcrUtils.getRepositoryByUri(repositoryFactory, uri);
		return super.getRepository();
	}

	public void remove() {
		try {
			Node remoteNode = userSession.getNode(remoteNodePath);
			remoteNode.remove();
			remoteNode.getSession().save();
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot remove " + remoteNodePath, e);
		}
	}

}
