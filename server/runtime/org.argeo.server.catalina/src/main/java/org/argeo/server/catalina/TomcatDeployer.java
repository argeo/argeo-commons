package org.argeo.server.catalina;

import org.apache.catalina.Service;
import org.springframework.osgi.web.deployer.tomcat.TomcatWarDeployer;

/**
 * Wraps the Spring DM Tomcate deployer in order to avoid issue with call to
 * getServerInfo() when undeployed.
 */
public class TomcatDeployer extends TomcatWarDeployer {
	private String serverInfo;

	@Override
	public void setService(Object service) {
		super.setService(service);

		serverInfo = ((Service) service).getInfo();
	}

	@Override
	protected String getServerInfo() {
		return serverInfo;
	}

	
}
