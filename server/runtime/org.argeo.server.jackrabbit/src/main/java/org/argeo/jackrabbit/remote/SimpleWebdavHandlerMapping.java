package org.argeo.jackrabbit.remote;

import java.util.Properties;

import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

/** Handler mapping for WebDav */
public class SimpleWebdavHandlerMapping extends
		AbstractJackrabbitHandlerMapping {
	private String configuration;

	protected HttpServlet createServlet(Repository repository, String pathPrefix)
			throws ServletException {

		SimpleWebdavServlet servlet = new SimpleWebdavServlet(repository,
				getSessionProvider());
		Properties initParameters = new Properties();
		initParameters.setProperty(
				SimpleWebdavServlet.INIT_PARAM_RESOURCE_CONFIG, configuration);
		initParameters
				.setProperty(
						SimpleWebdavServlet.INIT_PARAM_RESOURCE_PATH_PREFIX,
						pathPrefix);
		servlet.init(new DelegatingServletConfig(pathPrefix.replace('/', '_'),
				initParameters));
		return servlet;
	}

	public void setConfiguration(String configuration) {
		this.configuration = configuration;
	}
}
