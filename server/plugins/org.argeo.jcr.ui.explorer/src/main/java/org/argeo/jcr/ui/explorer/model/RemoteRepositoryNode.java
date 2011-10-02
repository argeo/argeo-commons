package org.argeo.jcr.ui.explorer.model;

import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.argeo.eclipse.ui.TreeParent;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.security.JcrKeyring;

/** Root of a remote repository */
public class RemoteRepositoryNode extends RepositoryNode {
	private JcrKeyring jcrKeyring;
	private String remoteNodePath;

	public RemoteRepositoryNode(String alias, Repository repository,
			TreeParent parent, JcrKeyring jcrKeyring, String remoteNodePath) {
		super(alias, repository, parent);
		this.jcrKeyring = jcrKeyring;
		this.remoteNodePath = remoteNodePath;
	}

	@Override
	protected Session repositoryLogin(String workspaceName)
			throws RepositoryException {
		Node remoteNode = jcrKeyring.getSession().getNode(remoteNodePath);
		String userID = remoteNode.getProperty(ArgeoNames.ARGEO_USER_ID)
				.getString();
		char[] password = jcrKeyring.getAsChars(remoteNodePath + "/"
				+ ArgeoNames.ARGEO_PASSWORD);
		try {
			SimpleCredentials credentials = new SimpleCredentials(userID,
					password);
			return getRepository().login(credentials, workspaceName);
		} finally {
			Arrays.fill(password, 0, password.length, ' ');
		}
	}

}
