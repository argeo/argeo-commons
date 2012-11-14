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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.security.auth.Subject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.core.DefaultSecurityManager;
import org.apache.jackrabbit.core.security.AMContext;
import org.apache.jackrabbit.core.security.AccessManager;
import org.apache.jackrabbit.core.security.AnonymousPrincipal;
import org.apache.jackrabbit.core.security.SecurityConstants;
import org.apache.jackrabbit.core.security.authorization.WorkspaceAccessManager;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;

/** Integrates Spring Security and Jackrabbit Security users and roles. */
public class ArgeoSecurityManager extends DefaultSecurityManager {
	/** Legacy security sync */
	final static String PROPERTY_JACKRABBIT_SECURITY_SYNC_1_1 = "argeo.jackarabbit.securitySync.1.1";

	private final static Log log = LogFactory
			.getLog(ArgeoSecurityManager.class);

	private static Boolean synchronize = Boolean.parseBoolean(System
			.getProperty(PROPERTY_JACKRABBIT_SECURITY_SYNC_1_1, "false"));

	/** TODO? use a bounded buffer */
	private Map<String, String> userRolesCache = Collections
			.synchronizedMap(new HashMap<String, String>());

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
		if (!synchronize)
			return super.getUserID(subject, workspaceName);

		if (log.isTraceEnabled())
			log.trace(subject);
		// skip anonymous user (no rights)
		if (!subject.getPrincipals(AnonymousPrincipal.class).isEmpty())
			return super.getUserID(subject, workspaceName);
		// skip Jackrabbit system user (all rights)
		if (!subject.getPrincipals(ArgeoSystemPrincipal.class).isEmpty())
			return super.getUserID(subject, workspaceName);

		// retrieve Spring authentication from JAAS
		// TODO? use Spring Security context holder
		Authentication authen;
		Set<Authentication> authens = subject
				.getPrincipals(Authentication.class);
		String userId = super.getUserID(subject, workspaceName);
		if (authens.size() == 0) {
			// make sure that logged-in user has a Principal, useful for testing
			// using an admin user
			UserManager systemUm = getSystemUserManager(null);
			if (systemUm.getAuthorizable(userId) == null)
				systemUm.createUser(userId, "");
		} else {// Spring Security
			authen = authens.iterator().next();

			if (!userId.equals(authen.getName()))
				log.warn("User ID is '" + userId + "' but authen is "
						+ authen.getName());
			StringBuffer roles = new StringBuffer("");
			GrantedAuthority[] authorities = authen.getAuthorities();
			for (GrantedAuthority ga : authorities) {
				roles.append(ga.toString());
			}

			// do not sync if not changed
			if (userRolesCache.containsKey(userId)
					&& userRolesCache.get(userId).equals(roles.toString()))
				return userId;

			// sync Spring and Jackrabbit
			// workspace is irrelevant here
			UserManager systemUm = getSystemUserManager(null);
			syncSpringAndJackrabbitSecurity(systemUm, authen);
			userRolesCache.put(userId, roles.toString());
		}
		return userId;
	}

	/**
	 * Make sure that the Jackrabbit security model contains this user and its
	 * granted authorities
	 */
	static private void syncSpringAndJackrabbitSecurity(UserManager systemUm,
			Authentication authen) throws RepositoryException {
		long begin = System.currentTimeMillis();

		String userId = authen.getName();
		User user = (User) systemUm.getAuthorizable(userId);
		if (user == null) {
			user = systemUm.createUser(userId, authen.getCredentials()
					.toString(), authen, null);
			log.info(userId + " added as " + user);
		}

		// process groups
		List<String> userGroupIds = new ArrayList<String>();
		for (GrantedAuthority ga : authen.getAuthorities()) {
			Group group = (Group) systemUm.getAuthorizable(ga.getAuthority());
			if (group == null) {
				group = systemUm.createGroup(ga.getAuthority());
				log.info(ga.getAuthority() + " added as " + group);
			}
			if (!group.isMember(user))
				group.addMember(user);
			userGroupIds.add(ga.getAuthority());
		}

		// check if user has not been removed from some groups
		for (Iterator<Group> it = user.declaredMemberOf(); it.hasNext();) {
			Group group = it.next();
			if (!userGroupIds.contains(group.getID()))
				group.removeMember(user);
		}

		if (log.isTraceEnabled())
			log.trace("Spring and Jackrabbit Security synchronized for user "
					+ userId + " in " + (System.currentTimeMillis() - begin)
					+ " ms");
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
