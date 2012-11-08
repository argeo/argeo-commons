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
import java.util.List;

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
import org.argeo.jcr.JcrUtils;
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

	public Session getSession(HttpServletRequest request, Repository rep,
			String workspace) throws LoginException, ServletException,
			RepositoryException {

		String springUser = SecurityContextHolder.getContext()
				.getAuthentication().getName();

		// HTTP
		String pathInfo = request.getPathInfo();
		List<String> tokens = JcrUtils.tokenize(pathInfo);
		String httpRepository = tokens.get(0);

		// HTTP session
		if (httpSession != null
				&& !httpSession.getId().equals(request.getSession().getId()))
			throw new ArgeoException(
					"Only session scope is supported in this mode");
		if (httpSession == null)
			httpSession = request.getSession();

		if (currentRepositoryName == null)
			currentRepositoryName = httpRepository;
		if (currentWorkspaceName == null)
			currentWorkspaceName = workspace;
		if (currentJcrUser == null)
			currentJcrUser = springUser;

		if (jcrSession != null)
			if (!currentRepositoryName.equals(httpRepository)) {
				if (log.isDebugEnabled())
					log.debug(getHttpSessionId() + " Changed from repository "
							+ currentRepositoryName + " to " + httpRepository
							+ ", logging out.");
				logout();
			} else if (!currentWorkspaceName.equals(workspace)) {
				if (log.isDebugEnabled())
					log.debug(getHttpSessionId() + " Changed from workspace "
							+ currentWorkspaceName + " to " + workspace
							+ ", logging out.");
				logout();
			} else if (!currentJcrUser.equals(springUser)) {
				if (log.isDebugEnabled())
					log.debug(getHttpSessionId() + " Changed from user "
							+ currentJcrUser + " to " + springUser
							+ ", logging out.");
				logout();
			}

		// JCR session
		if (jcrSession == null)
			try {
				Session session = login(rep, workspace);
				if (!session.getUserID().equals(springUser))
					throw new ArgeoException("HTTP user '" + springUser
							+ "' not in line with JCR user '"
							+ session.getUserID() + "'");
				currentRepositoryName = httpRepository;
				// do not use workspace variable which may be null
				currentWorkspaceName = session.getWorkspace().getName();
				currentJcrUser = session.getUserID();

				jcrSession = session;
				return jcrSession;
			} catch (RepositoryException e) {
				throw new ArgeoException("Cannot open session to workspace "
						+ workspace, e);
			}
		else
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
		if (log.isDebugEnabled())
			log.debug(getHttpSessionId() + " Releasing JCR session " + session);
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
		if (log.isDebugEnabled())
			log.debug(getHttpSessionId()
					+ " Cleaned up provider for web session ");
		httpSession = null;
	}
}