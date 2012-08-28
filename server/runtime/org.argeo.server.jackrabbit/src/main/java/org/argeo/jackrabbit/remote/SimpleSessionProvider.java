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
package org.argeo.jackrabbit.remote;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.LoginException;
import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.server.SessionProvider;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoNames;
import org.argeo.jcr.JcrUtils;
import org.argeo.jcr.UserJcrUtils;

/**
 * Implements an open session in view patter: a new JCR session is created for
 * each request
 */
public class SimpleSessionProvider implements SessionProvider, Serializable {
	private static final long serialVersionUID = 2270957712453841368L;

	private final static Log log = LogFactory
			.getLog(SimpleSessionProvider.class);

	private transient Map<String, Session> sessions;

	private Boolean openSessionInView = true;

	private String defaultWorkspace = "default";

	public Session getSession(HttpServletRequest request, Repository rep,
			String workspace) throws LoginException, ServletException,
			RepositoryException {

		if (openSessionInView) {
			JackrabbitSession session = (JackrabbitSession) rep
					.login(workspace);
			if (session.getWorkspace().getName().equals(defaultWorkspace))
				writeRemoteRoles(session);
			return session;
		} else {
			// since sessions is transient it can't be restored from the session
			if (sessions == null)
				sessions = Collections
						.synchronizedMap(new HashMap<String, Session>());

			if (!sessions.containsKey(workspace)) {
				try {
					JackrabbitSession session = (JackrabbitSession) rep.login(
							null, workspace);
					if (session.getWorkspace().getName()
							.equals(defaultWorkspace))
						writeRemoteRoles(session);
					if (log.isTraceEnabled())
						log.trace("User " + session.getUserID()
								+ " logged into " + request.getServletPath());
					sessions.put(workspace, session);
					return session;
				} catch (Exception e) {
					throw new ArgeoException("Cannot open session", e);
				}
			} else {
				Session session = sessions.get(workspace);
				if (!session.isLive()) {
					sessions.remove(workspace);
					session = rep.login(null, workspace);
					sessions.put(workspace, session);
				}
				return session;
			}
		}
	}

	protected void writeRemoteRoles(JackrabbitSession session)
			throws RepositoryException {
		// FIXME better deal w/ non node repo

		// retrieve roles
		String userId = session.getUserID();
		UserManager userManager = session.getUserManager();
		User user = (User) userManager.getAuthorizable(userId);
		if (user == null) {
			// anonymous
			return;
		}
		List<String> userGroupIds = new ArrayList<String>();
		if (user != null)
			for (Iterator<Group> it = user.memberOf(); it.hasNext();)
				userGroupIds.add(it.next().getID());

		// write roles if needed
		Node userHome = UserJcrUtils.getUserHome(session);
		boolean writeRoles = false;
		if (userHome.hasProperty(ArgeoNames.ARGEO_REMOTE_ROLES)) {
			Value[] roles = userHome.getProperty(ArgeoNames.ARGEO_REMOTE_ROLES)
					.getValues();
			if (roles.length != userGroupIds.size())
				writeRoles = true;
			else
				for (int i = 0; i < roles.length; i++)
					if (!roles[i].getString().equals(userGroupIds.get(i)))
						writeRoles = true;
		} else
			writeRoles = true;

		if (writeRoles) {
			session.getWorkspace().getVersionManager()
					.checkout(userHome.getPath());
			String[] roleIds = userGroupIds.toArray(new String[userGroupIds
					.size()]);
			userHome.setProperty(ArgeoNames.ARGEO_REMOTE_ROLES, roleIds);
			JcrUtils.updateLastModified(userHome);
			session.save();
			session.getWorkspace().getVersionManager()
					.checkin(userHome.getPath());
		}

	}

	public void releaseSession(Session session) {
		if (log.isTraceEnabled())
			log.trace("Releasing JCR session " + session);
		if (openSessionInView) {
			if (session.isLive()) {
				session.logout();
				if (log.isTraceEnabled())
					log.trace("Logged out remote JCR session " + session);
			}
		}
	}

	public void init() {
	}

	public void destroy() {
		if (sessions != null)
			for (String workspace : sessions.keySet()) {
				Session session = sessions.get(workspace);
				if (session.isLive()) {
					session.logout();
					if (log.isDebugEnabled())
						log.debug("Logged out remote JCR session " + session);
				}
			}
	}

	/**
	 * If set to true a new session will be created each time (the default),
	 * otherwise a single session is cached by workspace and the object should
	 * be of scope session (not supported)
	 */
	public void setOpenSessionInView(Boolean openSessionInView) {
		this.openSessionInView = openSessionInView;
	}

	public void setSecurityWorkspace(String securityWorkspace) {
		this.defaultWorkspace = securityWorkspace;
	}

}
