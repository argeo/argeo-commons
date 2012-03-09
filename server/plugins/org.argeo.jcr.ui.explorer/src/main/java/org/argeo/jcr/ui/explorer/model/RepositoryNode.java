/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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

public class RepositoryNode extends TreeParent implements UiNode {
	private String alias;
	private final Repository repository;
	private Session defaultSession = null;

	/** Create a new repository with alias = name */
	public RepositoryNode(String alias, Repository repository, TreeParent parent) {
		this(alias, alias, repository, parent);
	}

	/** Create a new repository with distinct name & alias */
	public RepositoryNode(String alias, String name, Repository repository,
			TreeParent parent) {
		super(name);
		this.repository = repository;
		setParent(parent);
		this.alias = alias;
	}

	public void login() {
		try {
			// SimpleCredentials sc = new SimpleCredentials("root",
			// "demo".toCharArray());
			// defaultSession = repository.login(sc);
			defaultSession = repositoryLogin(null);
			String[] wkpNames = defaultSession.getWorkspace()
					.getAccessibleWorkspaceNames();
			for (String wkpName : wkpNames) {
				if (wkpName.equals(defaultSession.getWorkspace().getName()))
					addChild(new WorkspaceNode(this, wkpName, defaultSession));
				else
					addChild(new WorkspaceNode(this, wkpName));
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot connect to repository " + alias, e);
		}
	}

	/** Actual call to the {@link Repository#login(javax.jcr.Credentials, String)} method. To be overridden.*/
	protected Session repositoryLogin(String workspaceName)
			throws RepositoryException {
		return repository.login(workspaceName);
	}

	public Session getDefaultSession() {
		return defaultSession;
	}

	/** returns the {@link Repository} referenced by the current UI Node */
	public Repository getRepository() {
		return repository;
	}

	public String getAlias() {
		return alias;
	}

}
