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
package org.argeo.eclipse.ui.workbench.internal.jcr.model;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
// import javax.jcr.Workspace;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.jcr.JcrUtils;

/**
 * UI Tree component. Wraps the root node of a JCR {@link Workspace}. It also
 * keeps a reference to its parent {@link RepositoryElem}, to be able to
 * retrieve alias of the current used repository
 */
public class WorkspaceElem extends TreeParent {
	private Session session = null;

	public WorkspaceElem(RepositoryElem parent, String name) {
		this(parent, name, null);
	}

	public WorkspaceElem(RepositoryElem parent, String name, Session session) {
		super(name);
		this.session = session;
		setParent(parent);
	}

	public synchronized Session getSession() {
		return session;
	}

	public synchronized Node getRootNode() {
		try {
			if (session != null)
				return session.getRootNode();
			else
				return null;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot get root node of workspace "
					+ getName(), e);
		}
	}

	public synchronized void login() {
		try {
			session = ((RepositoryElem) getParent()).repositoryLogin(getName());
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot connect to repository "
					+ getName(), e);
		}
	}

	public Boolean isConnected() {
		if (session != null && session.isLive())
			return true;
		else
			return false;
	}

	@Override
	public synchronized void dispose() {
		logout();
		super.dispose();
	}

	/** Logouts the session, does not nothing if there is no live session. */
	public synchronized void logout() {
		clearChildren();
		JcrUtils.logoutQuietly(session);
		session = null;
	}

	@Override
	public synchronized boolean hasChildren() {
		try {
			if (isConnected())
				return session.getRootNode().hasNodes();
			else
				return false;
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexpected error while checking children node existence",
					re);
		}
	}

	/** Override normal behaviour to initialize display of the workspace */
	@Override
	public synchronized Object[] getChildren() {
		if (isLoaded()) {
			return super.getChildren();
		} else {
			// initialize current object
			try {
				Node rootNode;
				if (session == null)
					return null;
				else
					rootNode = session.getRootNode();
				NodeIterator ni = rootNode.getNodes();
				while (ni.hasNext()) {
					Node node = ni.nextNode();
					addChild(new SingleJcrNodeElem(this, node, node.getName()));
				}
				return super.getChildren();
			} catch (RepositoryException e) {
				throw new ArgeoException(
						"Cannot initialize WorkspaceNode UI object."
								+ getName(), e);
			}
		}
	}
}
