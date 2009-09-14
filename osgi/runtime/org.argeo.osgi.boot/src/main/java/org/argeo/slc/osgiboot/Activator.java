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
//		try {
//			OsgiBoot.info("SLC OSGi bootstrap starting...");
//			osgiBoot.installUrls(osgiBoot.getBundlesUrls());
//			osgiBoot.installUrls(osgiBoot.getLocationsUrls());
//			osgiBoot.installUrls(osgiBoot.getModulesUrls());
//			osgiBoot.startBundles();
//			OsgiBoot.info("SLC OSGi bootstrap completed");
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw e;
//		}
	}

	public void stop(BundleContext context) throws Exception {
	}
}
