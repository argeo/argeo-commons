package org.argeo.eclipse.spring;

import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.springframework.context.ApplicationContext;

public abstract class AbstractSpringUiPlugin extends AbstractUIPlugin {
	private BundleContext bundleContext;

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		this.bundleContext = context;
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		super.stop(context);
	}

	public ApplicationContext getApplicationContext() {
		return ApplicationContextTracker.getApplicationContext(bundleContext
				.getBundle());
	}
}
