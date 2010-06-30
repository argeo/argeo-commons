/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
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

package org.argeo.server.jackrabbit.webdav;

import java.io.IOException;

import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.webdav.simple.ResourceConfig;
import org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet;
import org.argeo.ArgeoException;
import org.springframework.core.io.Resource;

public class WebDavServlet extends SimpleWebdavServlet {

	private static final long serialVersionUID = 1L;
	private final static Log log = LogFactory.getLog(WebDavServlet.class);

	private Repository repository;
	private Resource resourceConfiguration;

	public WebDavServlet() {

	}

	@Override
	public void init() throws ServletException {
		super.init();

		if (resourceConfiguration != null) {
			ResourceConfig resourceConfig = new ResourceConfig();
			try {
				resourceConfig.parse(resourceConfiguration.getURL());
			} catch (IOException e) {
				throw new ArgeoException("Cannot parse resource configuration "
						+ resourceConfiguration, e);
			}
			setResourceConfig(resourceConfig);
		}
	}

	@Override
	protected void service(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		if (log.isTraceEnabled())
			log.trace("Received request " + request);
		super.service(request, response);

		if (log.isTraceEnabled()) {
			log.trace("Webdav response: " + response);
			// response.
		}
	}

	public Repository getRepository() {
		return repository;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setResourceConfiguration(Resource resourceConfig) {
		this.resourceConfiguration = resourceConfig;
	}

}
