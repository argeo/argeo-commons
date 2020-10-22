package org.argeo.cms.ui.jcr.model;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.api.NodeConstants;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.jcr.JcrUtils;

/**
 * UI Tree component that wraps a JCR {@link Repository}. It also keeps a
 * reference to its parent Tree Ui component; typically the unique
 * {@link RepositoriesElem} object of the current view to enable bi-directionnal
 * browsing in the tree.
 */

public class RepositoryElem extends TreeParent {
	private String alias;
	protected Repository repository;
	private Session defaultSession = null;

	/** Create a new repository with distinct name and alias */
	public RepositoryElem(String alias, Repository repository, TreeParent parent) {
		super(alias);
		this.repository = repository;
		setParent(parent);
		this.alias = alias;
	}

	public void login() {
		try {
			defaultSession = repositoryLogin(NodeConstants.SYS_WORKSPACE);
			String[] wkpNames = defaultSession.getWorkspace().getAccessibleWorkspaceNames();
			for (String wkpName : wkpNames) {
				if (wkpName.equals(defaultSession.getWorkspace().getName()))
					addChild(new WorkspaceElem(this, wkpName, defaultSession));
				else
					addChild(new WorkspaceElem(this, wkpName));
			}
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot connect to repository " + alias, e);
		}
	}

	public synchronized void logout() {
		for (Object child : getChildren()) {
			if (child instanceof WorkspaceElem)
				((WorkspaceElem) child).logout();
		}
		clearChildren();
		JcrUtils.logoutQuietly(defaultSession);
		defaultSession = null;
	}

	/**
	 * Actual call to the {@link Repository#login(javax.jcr.Credentials, String)}
	 * method. To be overridden.
	 */
	protected Session repositoryLogin(String workspaceName) throws RepositoryException {
		return repository.login(workspaceName);
	}

	public String[] getAccessibleWorkspaceNames() {
		try {
			return defaultSession.getWorkspace().getAccessibleWorkspaceNames();
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot retrieve workspace names", e);
		}
	}

	public void createWorkspace(String workspaceName) {
		if (!isConnected())
			login();
		try {
			defaultSession.getWorkspace().createWorkspace(workspaceName);
		} catch (RepositoryException e) {
			throw new EclipseUiException("Cannot create workspace", e);
		}
	}

	/** returns the {@link Repository} referenced by the current UI Node */
	public Repository getRepository() {
		return repository;
	}

	public String getAlias() {
		return alias;
	}

	public Boolean isConnected() {
		if (defaultSession != null && defaultSession.isLive())
			return true;
		else
			return false;
	}
}
