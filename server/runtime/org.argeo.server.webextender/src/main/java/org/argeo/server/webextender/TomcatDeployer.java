package org.argeo.server.webextender;

import org.apache.catalina.Service;
import org.springframework.osgi.web.deployer.tomcat.TomcatWarDeployer;

/**
 * Wraps the Spring DM Tomcat deployer in order to avoid issue with call to
 * getServerInfo() when undeployed.
 */
public class TomcatDeployer extends TomcatWarDeployer {
	private String serverInfo;

	@Override
	public void setService(Object service) {
		super.setService(service);
		// TODO: listen to OSGi service so that we get notified in the
		// (unlikely) cae the underlying service is update
		serverInfo = ((Service) service).getInfo();
		if (log.isDebugEnabled())
			log.debug("Argeo modified Tomcat deployer used");
	}

	@Override
	protected String getServerInfo() {
		return serverInfo;
	}

}
