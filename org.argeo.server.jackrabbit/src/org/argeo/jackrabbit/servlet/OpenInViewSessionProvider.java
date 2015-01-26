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
package org.argeo.jackrabbit.servlet;

import java.io.Serializable;

import javax.jcr.LoginException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.server.SessionProvider;
import org.argeo.jcr.JcrUtils;

/**
 * Implements an open session in view patter: a new JCR session is created for
 * each request
 */
public class OpenInViewSessionProvider implements SessionProvider, Serializable {
	private static final long serialVersionUID = 2270957712453841368L;

	private final static Log log = LogFactory
			.getLog(OpenInViewSessionProvider.class);

	public Session getSession(HttpServletRequest request, Repository rep,
			String workspace) throws LoginException, ServletException,
			RepositoryException {
		return login(request, rep, workspace);
	}

	protected Session login(HttpServletRequest request, Repository repository,
			String workspace) throws RepositoryException {
		if (log.isTraceEnabled())
			log.trace("Login to workspace "
					+ (workspace == null ? "<default>" : workspace)
					+ " in web session " + request.getSession().getId());
		return repository.login(workspace);
	}

	public void releaseSession(Session session) {
		JcrUtils.logoutQuietly(session);
		if (log.isTraceEnabled())
			log.trace("Logged out remote JCR session " + session);
	}

	public void init() {
	}

	public void destroy() {
	}

}
