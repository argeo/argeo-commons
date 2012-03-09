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

import java.util.Hashtable;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.ErrorFeedback;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.RepositoryRegister;
import org.argeo.jcr.security.JcrKeyring;

/**
 * UI Tree component. Implements the Argeo abstraction of a
 * {@link RepositoryFactory} that enable a user to "mount" various repositories
 * in a single Tree like View. It is usually meant to be at the root of the UI
 * Tree and thus {@link getParent()} method will return null.
 * 
 * The {@link RepositoryFactory} is injected at instantiation time and must be
 * use get or register new {@link Repository} objects upon which a reference is
 * kept here.
 */

public class RepositoriesNode extends TreeParent implements ArgeoNames {
	private final RepositoryRegister repositoryRegister;
	private final RepositoryFactory repositoryFactory;

	private final JcrKeyring jcrKeyring;

	public RepositoriesNode(String name, RepositoryRegister repositoryRegister,
			RepositoryFactory repositoryFactory, TreeParent parent,
			JcrKeyring jcrKeyring) {
		super(name);
		this.repositoryRegister = repositoryRegister;
		this.repositoryFactory = repositoryFactory;
		this.jcrKeyring = jcrKeyring;
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
			Map<String, Repository> refRepos = repositoryRegister
					.getRepositories();
			for (String name : refRepos.keySet()) {
				super.addChild(new RepositoryNode(name, refRepos.get(name),
						this));
			}

			// remote
			if (jcrKeyring != null) {
				try {
					addRemoteRepositories(jcrKeyring);
				} catch (RepositoryException e) {
					throw new ArgeoException(
							"Cannot browse remote repositories", e);
				}
			}
			return super.getChildren();
		}
	}

	protected void addRemoteRepositories(JcrKeyring jcrKeyring)
			throws RepositoryException {
		Session userSession = jcrKeyring.getSession();
		Node userHome = JcrUtils.getUserHome(userSession);
		if (userHome != null && userHome.hasNode(ARGEO_REMOTE)) {
			NodeIterator it = userHome.getNode(ARGEO_REMOTE).getNodes();
			while (it.hasNext()) {
				Node remoteNode = it.nextNode();
				String uri = remoteNode.getProperty(ARGEO_URI).getString();
				try {
					Hashtable<String, String> params = new Hashtable<String, String>();
					params.put(ArgeoJcrConstants.JCR_REPOSITORY_URI, uri);
					params.put(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS,
							remoteNode.getName());
					Repository repository = repositoryFactory
							.getRepository(params);
					RemoteRepositoryNode remoteRepositoryNode = new RemoteRepositoryNode(
							remoteNode.getName(), repository, this, jcrKeyring,
							remoteNode.getPath());
					super.addChild(remoteRepositoryNode);
				} catch (Exception e) {
					ErrorFeedback.show("Cannot add remote repository "
							+ remoteNode, e);
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
