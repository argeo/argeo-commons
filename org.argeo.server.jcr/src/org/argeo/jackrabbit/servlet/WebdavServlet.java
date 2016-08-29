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
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;

/** WebDav servlet whose repository is injected */
@Deprecated
public class WebdavServlet extends SimpleWebdavServlet {
	public final static String INIT_PARAM_RESOURCE_CONFIG = SimpleWebdavServlet.INIT_PARAM_RESOURCE_CONFIG;
	public final static String INIT_PARAM_RESOURCE_PATH_PREFIX = SimpleWebdavServlet.INIT_PARAM_RESOURCE_PATH_PREFIX;

	private static final long serialVersionUID = -369787931175177080L;

	private final static Log log = LogFactory.getLog(WebdavServlet.class);

	private final Repository repository;

	public WebdavServlet(Repository repository, SessionProvider sessionProvider) {
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
