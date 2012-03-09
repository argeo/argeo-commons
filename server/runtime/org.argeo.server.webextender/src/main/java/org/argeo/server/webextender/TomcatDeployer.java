/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
	private String contextPath = "/org.argeo.rap.webapp";

	@Override
	public void setService(Object service) {
		this.service = (Service) service;
		super.setService(service);
		// TODO: listen to OSGi service so that we get notified in the
		// (unlikely) case the underlying service is updated
		serverInfo = ((Service) service).getInfo();
		if (log.isTraceEnabled())
			log.trace("Argeo modified Tomcat deployer used");
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
		Context context = getContext(contextPath);
		if (context != null)
			context.setCookies(false);
	}

	/** @return null if not found */
	private Context getContext(String path) {
		for (Container container : getHost().findChildren()) {
			if (log.isTraceEnabled())
				log.trace(container.getClass() + ": " + container.getName());
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

	public void setContextPath(String contextPath) {
		this.contextPath = contextPath;
	}

}
