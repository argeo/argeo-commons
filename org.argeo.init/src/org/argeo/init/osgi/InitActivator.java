package org.argeo.init.osgi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;

import org.argeo.api.init.RuntimeManager;
import org.argeo.init.ServiceMain;
import org.argeo.init.logging.ThinLoggerFinder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * An OSGi configurator. See
 * <a href="http://wiki.eclipse.org/Configurator">http:
 * //wiki.eclipse.org/Configurator</a>
 */
public class InitActivator implements BundleActivator {
	static {
		// must be called first
		ThinLoggerFinder.lazyInit();
	}
	private final static Logger logger = System.getLogger(InitActivator.class.getName());

	private Long checkpoint = null;

	private boolean argeoInit = false;
	/** Not null if we created it ourselves. */
	private OsgiRuntimeContext runtimeContext;

	private static OsgiRuntimeManager runtimeManager;

	public void start(final BundleContext bundleContext) throws Exception {
		// The OSGi runtime was created by us, and therefore already initialized
		argeoInit = Boolean.parseBoolean(bundleContext.getProperty(ServiceMain.PROP_ARGEO_INIT_MAIN));
		if (!argeoInit) {
			if (runtimeContext == null) {
				runtimeContext = new OsgiRuntimeContext(bundleContext);
				logger.log(Level.DEBUG, () -> "Argeo init via OSGi activator");
			}

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

		if (runtimeManager != null)
			throw new IllegalArgumentException("Runtime manager is already set");
		runtimeManager = new OsgiRuntimeManager(bundleContext);
	}

	public void stop(BundleContext context) throws Exception {
		if (!argeoInit) {
			Objects.nonNull(runtimeContext);
			runtimeContext.stop(context);
			runtimeContext = null;
		}
		runtimeManager = null;
	}

	public static RuntimeManager getRuntimeManager() {
		return runtimeManager;
	}

}
