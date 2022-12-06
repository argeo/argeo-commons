package org.argeo.cms.web.osgi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import org.argeo.api.cms.CmsApp;
import org.argeo.api.cms.CmsEventBus;
import org.argeo.cms.web.CmsWebApp;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/** Publish a CmsApp as a RAP application. */
public class CmsWebAppFactory {
	private BundleContext bundleContext = FrameworkUtil.getBundle(CmsWebAppFactory.class).getBundleContext();
	private final static String CONTEXT_NAME = "contextName";

	private CmsEventBus cmsEventBus;

	private Map<String, CmsWebApp> registrations = Collections.synchronizedMap(new HashMap<>());

	public void addCmsApp(CmsApp cmsApp, Map<String, String> properties) {
		String contextName = properties.get(CmsApp.CONTEXT_NAME_PROPERTY);
		if (contextName != null) {
			CmsWebApp cmsWebApp = new CmsWebApp();
			cmsWebApp.setCmsEventBus(cmsEventBus);
			cmsWebApp.setCmsApp(cmsApp);
			Hashtable<String, String> serviceProperties = new Hashtable<>();
			if (!contextName.equals(""))
				serviceProperties.put(CONTEXT_NAME, contextName);
			cmsWebApp.init(bundleContext, serviceProperties);
			registrations.put(contextName, cmsWebApp);
		}
	}

	public void removeCmsApp(CmsApp cmsApp, Map<String, String> properties) {
		String contextName = properties.get(CmsApp.CONTEXT_NAME_PROPERTY);
		if (contextName != null) {
			CmsWebApp cmsWebApp = registrations.get(contextName);
			if (cmsWebApp != null) {
				cmsWebApp.destroy(bundleContext, new HashMap<>());
				cmsWebApp.unsetCmsApp(cmsApp, properties);
			} else {
				// TODO log warning
			}
		}
	}

	public void setCmsEventBus(CmsEventBus cmsEventBus) {
		this.cmsEventBus = cmsEventBus;
	}


}
