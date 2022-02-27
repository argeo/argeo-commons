package org.argeo.init.osgi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;

import org.argeo.init.logging.ThinLoggerFinder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * An OSGi configurator. See
 * <a href="http://wiki.eclipse.org/Configurator">http:
 * //wiki.eclipse.org/Configurator</a>
 */
public class Activator implements BundleActivator {
	static {
		// must be called first
		ThinLoggerFinder.lazyInit();
	}
	private Logger logger = System.getLogger(Activator.class.getName());

	private Long checkpoint = null;
	private OsgiRuntimeContext runtimeContext;

	public void start(final BundleContext bundleContext) throws Exception {
		if (runtimeContext == null) {
			runtimeContext = new OsgiRuntimeContext(bundleContext);
		}
		logger.log(Level.DEBUG, () -> "Argeo init via OSGi activator");

		// admin thread
//		Thread adminThread = new AdminThread(bundleContext);
//		adminThread.start();

		// bootstrap
//		OsgiBoot osgiBoot = new OsgiBoot(bundleContext);
		if (checkpoint == null) {
//			osgiBoot.bootstrap();
			checkpoint = System.currentTimeMillis();
		} else {
			runtimeContext.update();
			checkpoint = System.currentTimeMillis();
		}
	}

	public void stop(BundleContext context) throws Exception {
		Objects.requireNonNull(runtimeContext);
		runtimeContext.stop(context);
	}
}
