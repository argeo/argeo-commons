package org.argeo.cms.ui.util;

import java.net.URL;
import java.util.Enumeration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.rap.rwt.application.Application;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

public class ThemeUtils {
	final static Log log = LogFactory.getLog(ThemeUtils.class);

	public static Bundle findThemeBundle(BundleContext bundleContext, String themeId) {
		if (themeId == null)
			return null;
		// TODO optimize
		// TODO deal with multiple versions
		Bundle themeBundle = null;
		if (themeId != null) {
			for (Bundle bundle : bundleContext.getBundles())
				if (themeId.equals(bundle.getSymbolicName())) {
					themeBundle = bundle;
					break;
				}
		}
		return themeBundle;
	}

	public static void addThemeResources(Application application, Bundle themeBundle, BundleResourceLoader themeBRL,
			String pattern) {
		Enumeration<URL> themeResources = themeBundle.findEntries("/", pattern, true);
		if (themeResources == null)
			return;
		while (themeResources.hasMoreElements()) {
			String resource = themeResources.nextElement().getPath();
			// remove first '/' so that RWT registers it
			resource = resource.substring(1);
			if (!resource.endsWith("/")) {
				application.addResource(resource, themeBRL);
				if (log.isTraceEnabled())
					log.trace("Registered " + resource + " from theme " + themeBundle);
			}

		}

	}

}
