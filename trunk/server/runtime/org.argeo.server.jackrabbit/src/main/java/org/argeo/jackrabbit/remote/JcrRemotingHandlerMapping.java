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

import java.util.Properties;

import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

public class JcrRemotingHandlerMapping extends AbstractJackrabbitHandlerMapping {

	protected HttpServlet createServlet(Repository repository, String pathPrefix)
			throws ServletException {
		JcrRemotingServlet servlet = new JcrRemotingServlet(repository,
				getSessionProvider());
		Properties initParameters = new Properties();
		initParameters.setProperty(
				JcrRemotingServlet.INIT_PARAM_RESOURCE_PATH_PREFIX, pathPrefix);
		servlet.init(new DelegatingServletConfig(pathPrefix.replace('/', '_'),
				initParameters));
		return servlet;
	}
}
