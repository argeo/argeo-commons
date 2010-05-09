package org.argeo.slc.osgiboot;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * An OSGi configurator. See <a
 * href="http://wiki.eclipse.org/Configurator">http:
 * //wiki.eclipse.org/Configurator</a>
 */
public class Activator implements BundleActivator {

	public void start(BundleContext bundleContext) throws Exception {
		OsgiBoot osgiBoot = new OsgiBoot(bundleContext);
		osgiBoot.bootstrap();
	}

	public void stop(BundleContext context) throws Exception {
	}
}
