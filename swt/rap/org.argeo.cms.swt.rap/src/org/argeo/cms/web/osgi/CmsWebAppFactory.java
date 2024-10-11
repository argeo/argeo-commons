package org.argeo.cms.web.osgi;

import static org.argeo.cms.web.osgi.RapOsgiConstants.HTTP_SERVICE_ENDPOINT;

import java.util.Collections;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.argeo.api.cms.CmsApp;
import org.argeo.api.cms.CmsEventBus;
import org.argeo.cms.web.CmsWebApp;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.ee10.servlet.ServletHolder;
import org.eclipse.rap.http.servlet.HttpServiceServlet;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.osgi.framework.BundleContext;

/** Publish a CmsApp as a RAP application. */
public class CmsWebAppFactory {
//	private final static int DEFAULT_MAX_INACTIVE_INTERVAL = 24 * 60 * 60;

	private BundleContext bundleContext; // = FrameworkUtil.getBundle(CmsWebAppFactory.class).getBundleContext();
	private final static String CONTEXT_NAME = "contextName";

	private CmsEventBus cmsEventBus;

	private Map<String, CmsWebApp> registrations = Collections.synchronizedMap(new HashMap<>());

	public void init(BundleContext bundleContext) {
		this.bundleContext = bundleContext;

//		publishJakartaServletContextHandler("slc/tool");
	}

	public void destroy() {
		bundleContext = null;
	}

	/*
	 * DEPENDENCIES INJECTION
	 */

	public void addCmsApp(CmsApp cmsApp, Map<String, String> properties) {
		String contextName = properties.get(CmsApp.CONTEXT_NAME_PROPERTY);
		if (contextName == null)
			return;

		publishJakartaServletContextHandler(contextName, null);

		CmsWebApp cmsWebApp = new CmsWebApp();
		cmsWebApp.setCmsEventBus(cmsEventBus);
		cmsWebApp.setCmsApp(cmsApp);
		Hashtable<String, String> serviceProperties = new Hashtable<>();
		if (!contextName.equals(""))
			serviceProperties.put(CONTEXT_NAME, contextName);
		cmsWebApp.init(bundleContext, serviceProperties);
		registrations.put(contextName, cmsWebApp);
	}

	public void removeCmsApp(CmsApp cmsApp, Map<String, String> properties) {
		String contextName = properties.get(CmsApp.CONTEXT_NAME_PROPERTY);
		if (contextName == null)
			return;

		CmsWebApp cmsWebApp = registrations.get(contextName);
		if (cmsWebApp != null) {
			cmsWebApp.destroy(bundleContext, new HashMap<>());
			cmsWebApp.unsetCmsApp(cmsApp, properties);
		} else {
			// TODO log warning
		}
	}

	public void addRwtApplication(ApplicationConfiguration applicationConfiguration, Map<String, String> properties) {
		String contextName = properties.get(CmsApp.CONTEXT_NAME_PROPERTY);
		// We only deal with plain RWT application (typically E4) when they are
		// explicitly published with an Argeo-specific context.
		// We don't want to be notified of the ones we have created ourselves.
		if (contextName == null)
			return;

		publishJakartaServletContextHandler(contextName, "(" + CmsApp.CONTEXT_NAME_PROPERTY + "=" + contextName + ")");
	}

	public void removeRwtApplication(ApplicationConfiguration applicationConfiguration,
			Map<String, String> properties) {
		// TODO unregister servlet context
	}
	/*
	 * UTILITIES
	 */

	void publishJakartaServletContextHandler(String contextName, String applicationConfigurationTarget) {
		String contextPath = "/" + contextName;

		ServletContextHandler servletContextHandler = new ServletContextHandler();
		servletContextHandler.setContextPath(contextPath);

		ServletHolder holder = new ServletHolder(new HttpServiceServlet());
		holder.setInitOrder(0);

//		SessionHandler sessionHandler = new SessionHandler();
//		// TODO make it configurable
//		//sessionHandler.setMaxInactiveInterval(DEFAULT_MAX_INACTIVE_INTERVAL);
//		servletContextHandler.setSessionHandler(sessionHandler);
//		try {
//			sessionHandler.addEventListener((HttpSessionIdListener) holder.getServlet());
//		} catch (ServletException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}

		// RAP OSGi matching (see org.eclipse.rap.rwt.osgi.internal.Matcher)
		// We over-do it, as only one of the two criteria would be enough.
		holder.setInitParameter(HTTP_SERVICE_ENDPOINT, contextPath);
		if (applicationConfigurationTarget == null)
			applicationConfigurationTarget = "(" + HTTP_SERVICE_ENDPOINT + "=/" + contextName + ")";
		holder.setInitParameter(RapOsgiConstants.APPLICATION_CONFIGURATION_TARGET, applicationConfigurationTarget);
		servletContextHandler.addServlet(holder, "/*");

		Dictionary<String, Object> regProps = new Hashtable<>();
		bundleContext.registerService(ServletContextHandler.class, servletContextHandler, regProps);
	}

	public void setCmsEventBus(CmsEventBus cmsEventBus) {
		this.cmsEventBus = cmsEventBus;
	}

}
