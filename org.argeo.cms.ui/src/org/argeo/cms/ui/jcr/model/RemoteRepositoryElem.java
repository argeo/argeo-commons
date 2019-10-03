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
package org.argeo.cms.ui.jcr.model;

import java.util.Arrays;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.argeo.cms.ArgeoNames;
import org.argeo.eclipse.ui.EclipseUiException;
import org.argeo.eclipse.ui.TreeParent;
import org.argeo.node.NodeUtils;
import org.argeo.node.security.Keyring;

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
			repository = NodeUtils.getRepositoryByUri(repositoryFactory, uri);
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
