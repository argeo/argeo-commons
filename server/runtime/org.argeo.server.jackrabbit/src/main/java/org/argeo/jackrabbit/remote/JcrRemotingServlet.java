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

import javax.jcr.Repository;

import org.apache.jackrabbit.server.SessionProvider;

public class JcrRemotingServlet extends
		org.apache.jackrabbit.server.remoting.davex.JcrRemotingServlet {

	private static final long serialVersionUID = 3131835511468341309L;

	private final Repository repository;
	private final SessionProvider sessionProvider;

	public JcrRemotingServlet(Repository repository,
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
