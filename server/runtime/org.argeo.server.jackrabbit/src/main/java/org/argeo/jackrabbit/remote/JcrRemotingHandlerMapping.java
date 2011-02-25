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
