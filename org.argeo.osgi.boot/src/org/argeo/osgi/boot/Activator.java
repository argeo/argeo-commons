package org.argeo.osgi.boot;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * An OSGi configurator. See
 * <a href="http://wiki.eclipse.org/Configurator">http:
 * //wiki.eclipse.org/Configurator</a>
 */
public class Activator implements BundleActivator {
	private Long checkpoint = null;

	public void start(final BundleContext bundleContext) throws Exception {
		// admin thread
		Thread adminThread = new AdminThread(bundleContext);
		adminThread.start();

		// bootstrap
		OsgiBoot osgiBoot = new OsgiBoot(bundleContext);
		if (checkpoint == null) {
			osgiBoot.bootstrap();
			checkpoint = System.currentTimeMillis();
		} else {
			osgiBoot.update();
			checkpoint = System.currentTimeMillis();
		}
	}

	public void stop(BundleContext context) throws Exception {
	}
}
