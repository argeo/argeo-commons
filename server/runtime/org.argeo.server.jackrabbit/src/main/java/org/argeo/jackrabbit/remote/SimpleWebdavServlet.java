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

import java.io.IOException;

import javax.jcr.Repository;
import javax.servlet.ServletException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.server.SessionProvider;
import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavResource;
import org.apache.jackrabbit.webdav.WebdavRequest;
import org.apache.jackrabbit.webdav.WebdavResponse;

/** WebDav servlet whose repository is injected */
public class SimpleWebdavServlet extends
		org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet {
	private static final long serialVersionUID = -369787931175177080L;

	private final static Log log = LogFactory.getLog(SimpleWebdavServlet.class);

	private final Repository repository;

	public SimpleWebdavServlet(Repository repository,
			SessionProvider sessionProvider) {
		this.repository = repository;
		setSessionProvider(sessionProvider);
	}

	public Repository getRepository() {
		return repository;
	}

	@Override
	protected boolean execute(WebdavRequest request, WebdavResponse response,
			int method, DavResource resource) throws ServletException,
			IOException, DavException {
		if (log.isTraceEnabled())
			log.trace(request.getMethod() + "\t" + request.getPathInfo());
		boolean res = super.execute(request, response, method, resource);
		return res;
	}

}
