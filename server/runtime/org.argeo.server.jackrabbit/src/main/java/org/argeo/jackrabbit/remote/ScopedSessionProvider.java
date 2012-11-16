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
package org.argeo.jackrabbit.remote;

import java.io.Serializable;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.server.SessionProvider;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.JcrUtils;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContextHolder;

/**
 * Session provider assuming a single workspace and a short life cycle,
 * typically a Spring bean of scope (web) 'session'.
 */
public class ScopedSessionProvider implements SessionProvider, Serializable {
	private static final long serialVersionUID = 6589775984177317058L;
	private static final Log log = LogFactory
			.getLog(ScopedSessionProvider.class);
	private transient HttpSession httpSession = null;
	private transient Session jcrSession = null;

	private transient String currentRepositoryName = null;
	private transient String currentWorkspaceName = null;
	private transient String currentJcrUser = null;

	// private transient String anonymousUserId = "anonymous";

	public Session getSession(HttpServletRequest request, Repository rep,
			String workspace) throws LoginException, ServletException,
			RepositoryException {

		Authentication authentication = SecurityContextHolder.getContext()
				.getAuthentication();
		if (authentication == null)
			throw new ArgeoException(
					"Request not authenticated by Spring Security");
		String springUser = authentication.getName();

		// HTTP
		String requestJcrRepository = (String) request
				.getAttribute(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS);

		// HTTP session
		if (httpSession != null
				&& !httpSession.getId().equals(request.getSession().getId()))
			throw new ArgeoException(
					"Only session scope is supported in this mode");
		if (httpSession == null)
			httpSession = request.getSession();

		// Initializes current values
		if (currentRepositoryName == null)
			currentRepositoryName = requestJcrRepository;
		if (currentWorkspaceName == null)
			currentWorkspaceName = workspace;
		if (currentJcrUser == null)
			currentJcrUser = springUser;

		// logout if there was a change in session coordinates
		if (jcrSession != null)
			if (!currentRepositoryName.equals(requestJcrRepository)) {
				if (log.isDebugEnabled())
					log.debug(getHttpSessionId() + " Changed from repository '"
							+ currentRepositoryName + "' to '"
							+ requestJcrRepository
							+ "', logging out cached JCR session.");
				logout();
			} else if (!currentWorkspaceName.equals(workspace)) {
				if (log.isDebugEnabled())
					log.debug(getHttpSessionId() + " Changed from workspace '"
							+ currentWorkspaceName + "' to '" + workspace
							+ "', logging out cached JCR session.");
				logout();
			} else if (!currentJcrUser.equals(springUser)) {
				if (log.isDebugEnabled())
					log.debug(getHttpSessionId() + " Changed from user '"
							+ currentJcrUser + "' to '" + springUser
							+ "', logging out cached JCR session.");
				logout();
			}

		// login if needed
		if (jcrSession == null)
			try {
				Session session = login(rep, workspace);
				if (!session.getUserID().equals(springUser)) {
					JcrUtils.logoutQuietly(session);
					throw new ArgeoException("Spring Security user '"
							+ springUser + "' not in line with JCR user '"
							+ session.getUserID() + "'");
				}
				currentRepositoryName = requestJcrRepository;
				// do not use workspace variable which may be null
				currentWorkspaceName = session.getWorkspace().getName();
				currentJcrUser = session.getUserID();

				jcrSession = session;
				return jcrSession;
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot open session to workspace "
						+ workspace, e);
			}

		// returns cached session
		return jcrSession;
	}

	protected Session login(Repository repository, String workspace)
			throws RepositoryException {
		Session session = repository.login(workspace);
		if (log.isDebugEnabled())
			log.debug(getHttpSessionId() + " User '" + session.getUserID()
					+ "' logged in workspace '"
					+ session.getWorkspace().getName() + "' of repository '"
					+ currentRepositoryName + "'");
		return session;
	}

	public void releaseSession(Session session) {
		if (log.isTraceEnabled())
			log.trace(getHttpSessionId() + " Releasing JCR session " + session);
	}

	protected void logout() {
		JcrUtils.logoutQuietly(jcrSession);
		jcrSession = null;
	}

	protected final String getHttpSessionId() {
		return httpSession != null ? httpSession.getId() : "<null>";
	}

	public void init() {
	}

	public void destroy() {
		logout();
		if (getHttpSessionId() != null)
			if (log.isDebugEnabled())
				log.debug(getHttpSessionId()
						+ " Cleaned up provider for web session ");
		httpSession = null;
	}

}
