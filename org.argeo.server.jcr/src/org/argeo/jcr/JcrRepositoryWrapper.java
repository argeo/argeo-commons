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
package org.argeo.jcr;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * Wrapper around a JCR repository which allows to simplify configuration and
 * intercept some actions. It exposes itself as a {@link Repository}.
 */
public abstract class JcrRepositoryWrapper implements Repository {
	// private final static Log log = LogFactory
	// .getLog(JcrRepositoryWrapper.class);

	// wrapped repository
	private Repository repository;

	private Boolean autocreateWorkspaces = false;

	/**
	 * Empty constructor, {@link #init()} should be called after properties have
	 * been set
	 */
	public JcrRepositoryWrapper() {
	}

	/** Initializes */
	public void init() {
	}

	/** Shutdown the repository */
	public void destroy() throws Exception {
	}

	/*
	 * DELEGATED JCR REPOSITORY METHODS
	 */

	public String getDescriptor(String key) {
		return getRepository().getDescriptor(key);
	}

	public String[] getDescriptorKeys() {
		return getRepository().getDescriptorKeys();
	}

	/** Central login method */
	public Session login(Credentials credentials, String workspaceName)
			throws LoginException, NoSuchWorkspaceException,
			RepositoryException {
		Session session;
		try {
			session = getRepository().login(credentials, workspaceName);
		} catch (NoSuchWorkspaceException e) {
			if (autocreateWorkspaces && workspaceName != null)
				session = createWorkspaceAndLogsIn(credentials, workspaceName);
			else
				throw e;
		}
		processNewSession(session);
		return session;
	}

	public Session login() throws LoginException, RepositoryException {
		return login(null, null);
	}

	public Session login(Credentials credentials) throws LoginException,
			RepositoryException {
		return login(credentials, null);
	}

	public Session login(String workspaceName) throws LoginException,
			NoSuchWorkspaceException, RepositoryException {
		return login(null, workspaceName);
	}

	/** Called after a session has been created, does nothing by default. */
	protected void processNewSession(Session session) {
	}

	/** Wraps access to the repository, making sure it is available. */
	protected synchronized Repository getRepository() {
//		if (repository == null) {
//			throw new ArgeoJcrException("No repository initialized."
//					+ " Was the init() method called?"
//					+ " The destroy() method should also"
//					+ " be called on shutdown.");
//		}
		return repository;
	}

	/**
	 * Logs in to the default workspace, creates the required workspace, logs
	 * out, logs in to the required workspace.
	 */
	protected Session createWorkspaceAndLogsIn(Credentials credentials,
			String workspaceName) throws RepositoryException {
		if (workspaceName == null)
			throw new ArgeoJcrException("No workspace specified.");
		Session session = getRepository().login(credentials);
		session.getWorkspace().createWorkspace(workspaceName);
		session.logout();
		return getRepository().login(credentials, workspaceName);
	}

	public boolean isStandardDescriptor(String key) {
		return getRepository().isStandardDescriptor(key);
	}

	public boolean isSingleValueDescriptor(String key) {
		return getRepository().isSingleValueDescriptor(key);
	}

	public Value getDescriptorValue(String key) {
		return getRepository().getDescriptorValue(key);
	}

	public Value[] getDescriptorValues(String key) {
		return getRepository().getDescriptorValues(key);
	}

	public synchronized void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setAutocreateWorkspaces(Boolean autocreateWorkspaces) {
		this.autocreateWorkspaces = autocreateWorkspaces;
	}

}
