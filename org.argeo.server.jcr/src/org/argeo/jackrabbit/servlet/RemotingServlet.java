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

import javax.jcr.Repository;

import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.server.remoting.davex.JcrRemotingServlet;

/** Provides remote access to a JCR repository */
public class RemotingServlet extends JcrRemotingServlet {
	public final static String INIT_PARAM_RESOURCE_PATH_PREFIX = JcrRemotingServlet.INIT_PARAM_RESOURCE_PATH_PREFIX;
	public final static String INIT_PARAM_HOME = JcrRemotingServlet.INIT_PARAM_HOME;
	public final static String INIT_PARAM_TMP_DIRECTORY = JcrRemotingServlet.INIT_PARAM_TMP_DIRECTORY;
	public final static String INIT_PARAM_PROTECTED_HANDLERS_CONFIG = JcrRemotingServlet.INIT_PARAM_PROTECTED_HANDLERS_CONFIG;

	private static final long serialVersionUID = 3131835511468341309L;

	private final Repository repository;
	private final SessionProvider sessionProvider;

	public RemotingServlet(Repository repository,
			SessionProvider sessionProvider) {
		this.repository = repository;
		this.sessionProvider = sessionProvider;
	}

	@Override
	protected Repository getRepository() {
		return repository;
	}

	@Override
	protected SessionProvider getSessionProvider() {
		return sessionProvider;
	}

}
