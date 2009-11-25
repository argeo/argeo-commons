package org.argeo.server.jackrabbit.webdav;

import java.io.IOException;

import javax.jcr.Repository;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.webdav.simple.ResourceConfig;
import org.argeo.ArgeoException;
import org.springframework.core.io.Resource;

public class SimpleWebDavServlet extends
		org.apache.jackrabbit.webdav.simple.SimpleWebdavServlet {

	private static final long serialVersionUID = 1L;
	private final static Log log = LogFactory.getLog(SimpleWebDavServlet.class);

	/**
	 * the jcr repository
	 */
	private Repository repository;
	private Resource resourceConfiguration;

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
		if (log.isDebugEnabled())
			log.debug("Received request with method '" + request.getMethod()
					+ "'");
		super.service(request, response);
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
