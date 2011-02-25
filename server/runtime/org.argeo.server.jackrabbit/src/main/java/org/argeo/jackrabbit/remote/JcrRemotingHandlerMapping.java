package org.argeo.jackrabbit.remote;

import java.util.Properties;

import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import org.apache.jackrabbit.webdav.jcr.JCRWebdavServerServlet;
import org.argeo.jcr.mvc.MultipleRepositoryHandlerMapping;

public class JcrRemotingHandlerMapping extends MultipleRepositoryHandlerMapping {
	protected HttpServlet createServlet(Repository repository, String pathPrefix)
			throws ServletException {
		JcrRemotingServlet servlet = new JcrRemotingServlet(repository);
		Properties initParameters = new Properties();
		initParameters.setProperty(
				JCRWebdavServerServlet.INIT_PARAM_RESOURCE_PATH_PREFIX,
				pathPrefix);
		servlet.init(new DelegatingServletConfig(pathPrefix.replace('/', '_'),
				initParameters));
		return servlet;
	}
}
