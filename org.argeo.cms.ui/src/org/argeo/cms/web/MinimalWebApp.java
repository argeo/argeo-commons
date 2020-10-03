package org.argeo.cms.web;

import static org.argeo.cms.ui.util.CmsTheme.CMS_THEME_BUNDLE_PROPERTY;

import java.util.HashMap;
import java.util.Map;

import org.argeo.cms.ui.util.CmsTheme;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.eclipse.rap.rwt.client.WebClient;
import org.osgi.framework.BundleContext;

/** Lightweight web app using only RWT and not the whole Eclipse platform. */
public class MinimalWebApp implements ApplicationConfiguration {

	private CmsTheme theme;

	public void init(BundleContext bundleContext, Map<String, Object> properties) {
		if (properties.containsKey(CMS_THEME_BUNDLE_PROPERTY)) {
			String cmsThemeBundle = properties.get(CMS_THEME_BUNDLE_PROPERTY).toString();
			theme = new CmsTheme(bundleContext, cmsThemeBundle);
		}
	}

	public void destroy() {

	}

	/** To be overridden. Does nothing by default. */
	protected void addEntryPoints(Application application, Map<String, String> properties) {

	}

	@Override
	public void configure(Application application) {
		if (theme != null)
			theme.apply(application);

		Map<String, String> properties = new HashMap<>();
		if (theme != null) {
			properties.put(WebClient.THEME_ID, theme.getThemeId());
			properties.put(WebClient.HEAD_HTML, theme.getAdditionalHeaders());
		} else {
			properties.put(WebClient.THEME_ID, RWT.DEFAULT_THEME_ID);
		}
		addEntryPoints(application, properties);

	}
}
