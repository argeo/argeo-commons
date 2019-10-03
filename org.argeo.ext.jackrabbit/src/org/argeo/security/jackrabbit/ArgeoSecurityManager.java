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
import org.apache.jackrabbit.core.security.SystemPrincipal;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.apache.jackrabbit.core.security.principal.AdminPrincipal;
import org.argeo.node.NodeConstants;
import org.argeo.node.security.AnonymousPrincipal;
import org.argeo.node.security.DataAdminPrincipal;

/** Customises Jackrabbit security. */
public class ArgeoSecurityManager extends DefaultSecurityManager {
	@Override
	public AccessManager getAccessManager(Session session, AMContext amContext) throws RepositoryException {
		synchronized (getSystemSession()) {
			return super.getAccessManager(session, amContext);
		}
	}

	@Override
	public UserManager getUserManager(Session session) throws RepositoryException {
		synchronized (getSystemSession()) {
			return super.getUserManager(session);
		}
	}

	/** Called once when the session is created */
	@Override
	public String getUserID(Subject subject, String workspaceName) throws RepositoryException {
		boolean isAnonymous = !subject.getPrincipals(AnonymousPrincipal.class).isEmpty();
		boolean isDataAdmin = !subject.getPrincipals(DataAdminPrincipal.class).isEmpty();
		boolean isJackrabbitSystem = !subject.getPrincipals(SystemPrincipal.class).isEmpty();
		Set<X500Principal> userPrincipal = subject.getPrincipals(X500Principal.class);
		boolean isRegularUser = !userPrincipal.isEmpty();
		if (isAnonymous) {
			if (isDataAdmin || isJackrabbitSystem || isRegularUser)
				throw new IllegalStateException("Inconsistent " + subject);
			else
				return NodeConstants.ROLE_ANONYMOUS;
		} else if (isRegularUser) {// must be before DataAdmin
			if (isAnonymous || isJackrabbitSystem)
				throw new IllegalStateException("Inconsistent " + subject);
			else {
				if (userPrincipal.size() > 1) {
					StringBuilder buf = new StringBuilder();
					for (X500Principal principal : userPrincipal)
						buf.append(' ').append('\"').append(principal).append('\"');
					throw new RuntimeException("Multiple user principals:" + buf);
				}
				return userPrincipal.iterator().next().getName();
			}
		} else if (isDataAdmin) {
			if (isAnonymous || isJackrabbitSystem || isRegularUser)
				throw new IllegalStateException("Inconsistent " + subject);
			else {
				assert !subject.getPrincipals(AdminPrincipal.class).isEmpty();
				return NodeConstants.ROLE_DATA_ADMIN;
			}
		} else if (isJackrabbitSystem) {
			if (isAnonymous || isDataAdmin || isRegularUser)
				throw new IllegalStateException("Inconsistent " + subject);
			else
				return super.getUserID(subject, workspaceName);
		} else {
			throw new IllegalStateException("Unrecognized subject type: " + subject);
		}
	}

	@Override
	protected WorkspaceAccessManager createDefaultWorkspaceAccessManager() {
		WorkspaceAccessManager wam = super.createDefaultWorkspaceAccessManager();
		return new ArgeoWorkspaceAccessManagerImpl(wam);
	}

	private class ArgeoWorkspaceAccessManagerImpl implements SecurityConstants, WorkspaceAccessManager {
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

		public boolean grants(Set<Principal> principals, String workspaceName) throws RepositoryException {
			// TODO: implements finer access to workspaces
			return true;
		}
	}

}
