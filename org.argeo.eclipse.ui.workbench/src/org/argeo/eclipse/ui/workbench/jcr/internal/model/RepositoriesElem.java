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
package org.argeo.eclipse.ui.workbench.jcr.internal.model;

import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.MaintainedRepository;
import org.argeo.jcr.RepositoryRegister;
import org.argeo.jcr.UserJcrUtils;
import org.argeo.util.security.Keyring;

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

	public RepositoriesElem(String name, RepositoryRegister repositoryRegister,
			RepositoryFactory repositoryFactory, TreeParent parent,
			Session userSession, Keyring keyring) {
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
			Map<String, Repository> refRepos = repositoryRegister
					.getRepositories();
			for (String name : refRepos.keySet()) {
				Repository repository = refRepos.get(name);
				if (repository instanceof MaintainedRepository)
					super.addChild(new MaintainedRepositoryElem(name,
							repository, this));
				else
					super.addChild(new RepositoryElem(name, repository, this));
			}

			// remote
			if (keyring != null) {
				try {
					addRemoteRepositories(keyring);
				} catch (RepositoryException e) {
					throw new ArgeoException(
							"Cannot browse remote repositories", e);
				}
			}
			return super.getChildren();
		}
	}

	protected void addRemoteRepositories(Keyring jcrKeyring)
			throws RepositoryException {
		Node userHome = UserJcrUtils.getUserHome(userSession);
		if (userHome != null && userHome.hasNode(ARGEO_REMOTE)) {
			NodeIterator it = userHome.getNode(ARGEO_REMOTE).getNodes();
			while (it.hasNext()) {
				Node remoteNode = it.nextNode();
				String uri = remoteNode.getProperty(ARGEO_URI).getString();
				try {
					RemoteRepositoryElem remoteRepositoryNode = new RemoteRepositoryElem(
							remoteNode.getName(), repositoryFactory, uri, this,
							userSession, jcrKeyring, remoteNode.getPath());
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
