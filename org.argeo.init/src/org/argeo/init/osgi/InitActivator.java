package org.argeo.init.osgi;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

import org.argeo.api.init.RuntimeManager;
import org.argeo.init.logging.ThinLoggerFinder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.connect.ConnectFrameworkFactory;
import org.osgi.framework.launch.Framework;

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

	// TODO use framework factory SR
//	@Deprecated
//	private boolean argeoInit = false;
	/** Not null if we created it ourselves. */
	private OsgiRuntimeContext runtimeContext;
	private ServiceRegistration<ConnectFrameworkFactory> frameworkFactorySr = null;

	private static OsgiRuntimeManager runtimeManager;

	public void start(final BundleContext bundleContext) throws Exception {
		ConnectFrameworkFactory frameworkFactory = OsgiRuntimeContext.getFrameworkFactory(bundleContext);
		if (frameworkFactory == null) {
//			argeoInit = false;
			frameworkFactory = newFrameworkFactory();
			frameworkFactorySr = bundleContext.registerService(ConnectFrameworkFactory.class, frameworkFactory, null);
		}

		// The OSGi runtime was created by us, and therefore already initialized
//		argeoInit = Boolean.parseBoolean(bundleContext.getProperty(ServiceMain.PROP_ARGEO_INIT_MAIN));
		if (!isArgeoInit()) {
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
		} else {

			if (runtimeManager != null)
				throw new IllegalArgumentException("Runtime manager is already set");
			runtimeManager = new OsgiRuntimeManager(bundleContext);
		}
	}

	public void stop(BundleContext context) throws Exception {
		if (!isArgeoInit()) {
			frameworkFactorySr.unregister();
			Objects.nonNull(runtimeContext);
			runtimeContext.stop(context);
			runtimeContext = null;
		}
		runtimeManager = null;
	}

	/** Whether it wa sinitialised by an Argeo Init main class. */
	private boolean isArgeoInit() {
		return frameworkFactorySr == null;
	}

	public static RuntimeManager getRuntimeManager() {
		return runtimeManager;
	}

	/**
	 * Workaround to explicitly instantiate an Equinox
	 * {@link ConnectFrameworkFactory} when running in a pure OSGi runtime.
	 */
	private ConnectFrameworkFactory newFrameworkFactory() {
		final String EQUINOX_FRAMEWORK_FACTORY_CLASS = "org.eclipse.osgi.launch.EquinoxFactory";
		try {
			@SuppressWarnings("unchecked")
			Class<? extends ConnectFrameworkFactory> frameworkFactoryClass = (Class<? extends ConnectFrameworkFactory>) Framework.class
					.getClassLoader().loadClass(EQUINOX_FRAMEWORK_FACTORY_CLASS);
			return frameworkFactoryClass.getConstructor().newInstance();
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new IllegalStateException("Cannot create OSGi framework factory", e);
		}
	}
}
