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
package org.argeo.security.jackrabbit;

import java.security.Principal;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;

import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.DefaultSecurityManager;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;

/** Integrates Spring Security and Jackrabbit Security users and roles. */
public class ArgeoSecurityManager extends DefaultSecurityManager {
	@Override
	public AccessManager getAccessManager(Session session, AMContext amContext)
			throws RepositoryException {
		synchronized (getSystemSession()) {
			return super.getAccessManager(session, amContext);
		}
	}

	@Override
	public UserManager getUserManager(Session session)
			throws RepositoryException {
		synchronized (getSystemSession()) {
			return super.getUserManager(session);
		}
	}

	/**
	 * Since this is called once when the session is created, we take the
	 * opportunity to make sure that Jackrabbit users and groups reflect Spring
	 * Security name and authorities.
	 */
	@Override
	public String getUserID(Subject subject, String workspaceName)
			throws RepositoryException {
		Set<X500Principal> userPrincipal = subject
				.getPrincipals(X500Principal.class);
		if (userPrincipal.isEmpty())
			return super.getUserID(subject, workspaceName);
		if (userPrincipal.size() > 1)
			throw new RuntimeException("Multiple user principals "
					+ userPrincipal);
		return userPrincipal.iterator().next().getName();
		// Authentication authentication = SecurityContextHolder.getContext()
		// .getAuthentication();
		// if (authentication != null)
		// return authentication.getName();
		// else
		// return super.getUserID(subject, workspaceName);
	}

	@Override
	protected WorkspaceAccessManager createDefaultWorkspaceAccessManager() {
		WorkspaceAccessManager wam = super
				.createDefaultWorkspaceAccessManager();
		return new ArgeoWorkspaceAccessManagerImpl(wam);
	}

	private class ArgeoWorkspaceAccessManagerImpl implements SecurityConstants,
			WorkspaceAccessManager {
		private final WorkspaceAccessManager wam;

		public ArgeoWorkspaceAccessManagerImpl(WorkspaceAccessManager wam) {
			super();
			this.wam = wam;
		}

		public void init(Session systemSession) throws RepositoryException {
			wam.init(systemSession);
		}

		public void close() throws RepositoryException {
		}

		public boolean grants(Set<Principal> principals, String workspaceName)
				throws RepositoryException {
			// TODO: implements finer access to workspaces
			return true;
		}
	}

}
