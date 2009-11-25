package org.argeo.server.jackrabbit.webdav;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.jackrabbit.webdav.simple.ResourceConfig;
import org.argeo.ArgeoException;
import org.springframework.core.io.Resource;

public class SimpleWebDavServlet extends
		org.apache.jackrabbit.j2ee.SimpleWebdavServlet {

	private static final long serialVersionUID = 1L;

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

	public void setResourceConfiguration(Resource resourceConfig) {
		this.resourceConfiguration = resourceConfig;
	}

}
