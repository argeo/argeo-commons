package org.argeo.cms.web;

import java.util.HashMap;
import java.util.Map;

import org.argeo.cms.ui.util.CmsTheme;
import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.application.AbstractEntryPoint;
import org.eclipse.rap.rwt.application.Application;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.eclipse.rap.rwt.client.WebClient;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.osgi.framework.BundleContext;

/** Lightweight web app using only RWT and not the whole Eclipse platform. */
public class MinimalWebApp implements ApplicationConfiguration {

	private CmsTheme theme;

	public void init(BundleContext bundleContext) {
		theme = new CmsTheme(bundleContext);
	}

	public void destroy() {

	}

	@Override
	public void configure(Application application) {
		theme.apply(application);

		Map<String, String> properties = new HashMap<>();
		properties.put(WebClient.THEME_ID, RWT.DEFAULT_THEME_ID);
		properties.put(WebClient.HEAD_HTML, theme.getAdditionalHeaders());
		application.addEntryPoint("/test", TextEntryPoint.class, properties);

	}

	static class TextEntryPoint extends AbstractEntryPoint {
		private static final long serialVersionUID = 2245808564950897823L;

		@Override
		protected void createContents(Composite parent) {
			parent.setLayout(new GridLayout());
			new Label(parent, SWT.NONE).setText("Hello World!");
		}

	}

}
