/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.jcr.ui.explorer.model;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.TreeParent;

/**
 * UI Tree component. Wraps a JCR {@link Repository}. It also keeps a reference
 * to its parent Tree Ui component; typically the unique {@link Repositories}
 * object of the current view to enable bi-directionnal browsing in the tree.
 */

public class RepositoryElem extends TreeParent {
	private String alias;
	protected Repository repository;
	private Session defaultSession = null;

	/** Create a new repository with distinct name & alias */
	public RepositoryElem(String alias, Repository repository, TreeParent parent) {
		super(alias);
		this.repository = repository;
		setParent(parent);
		this.alias = alias;
	}

	public void login() {
		try {
			defaultSession = repositoryLogin(null);
			String[] wkpNames = defaultSession.getWorkspace()
					.getAccessibleWorkspaceNames();
			for (String wkpName : wkpNames) {
				if (wkpName.equals(defaultSession.getWorkspace().getName()))
					addChild(new WorkspaceElem(this, wkpName, defaultSession));
				else
					addChild(new WorkspaceElem(this, wkpName));
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot connect to repository " + alias, e);
		}
	}

	/**
	 * Actual call to the
	 * {@link Repository#login(javax.jcr.Credentials, String)} method. To be
	 * overridden.
	 */
	protected Session repositoryLogin(String workspaceName)
			throws RepositoryException {
		return repository.login(workspaceName);
	}

	public String[] getAccessibleWorkspaceNames() {
		try {
			return defaultSession.getWorkspace().getAccessibleWorkspaceNames();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot retrieve workspace names", e);
		}
	}

	public void createWorkspace(String workspaceName) {
		if (!isConnected())
			login();
		try {
			defaultSession.getWorkspace().createWorkspace(workspaceName);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot create workspace", e);
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
