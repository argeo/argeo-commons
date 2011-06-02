package org.argeo.server.webextender;

import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.Service;
import org.springframework.osgi.web.deployer.WarDeployment;
import org.springframework.osgi.web.deployer.tomcat.TomcatWarDeployer;
import org.springframework.util.ObjectUtils;

/**
 * Wraps the Spring DM Tomcat deployer in order to avoid issue with call to
 * getServerInfo() when undeployed. We need to hack a lot here because Spring
 * OSGi Web is really not extendable.
 */
public class TomcatDeployer extends TomcatWarDeployer {
	private String serverInfo;
	private Service service;

	@Override
	public void setService(Object service) {
		this.service = (Service) service;
		super.setService(service);
		// TODO: listen to OSGi service so that we get notified in the
		// (unlikely) case the underlying service is updated
		serverInfo = ((Service) service).getInfo();
		if (log.isDebugEnabled())
			log.debug("Argeo modified Tomcat deployer used");
	}

	@Override
	protected String getServerInfo() {
		return serverInfo;
	}

	@Override
	protected void startDeployment(WarDeployment deployment) throws Exception {
		// Context context = ((TomcatWarDeployment)
		// deployment).getCatalinaContext();
		// context.setCookies(false);
		super.startDeployment(deployment);

		// Required for multiple RAP sessions to work with Tomcat
		// see
		// http://wiki.eclipse.org/RAP/FAQ#How_to_run_a_RAP_application_in_multiple_browser_tabs.2Fwindows.3F
		// TODO make it configurable in order to cover other web apps
		Context context = getContext("/org.argeo.rap.webapp");
		if (context != null)
			context.setCookies(false);
	}

	/** @return null if not found */
	private Context getContext(String path) {
		for (Container container : getHost().findChildren()) {
			log.debug(container.getClass() + ": " + container.getName());
			if (container instanceof Context) {
				Context context = (Context) container;
				if (path.equals(context.getPath()))
					return context;
			}
		}
		return null;
	}

	private Container getHost() {
		// get engine
		Container container = service.getContainer();

		if (container == null)
			throw new IllegalStateException(
					"The Tomcat server doesn't have any Engines defined");
		// now get host
		Container[] children = container.findChildren();
		if (ObjectUtils.isEmpty(children))
			throw new IllegalStateException(
					"The Tomcat server doesn't have any Hosts defined");

		// pick the first one and associate the context with it
		return children[0];
	}

}
