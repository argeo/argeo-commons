package org.argeo.init.osgi;

import java.io.Serializable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.System.LoggerFinder;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Flow;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.argeo.api.init.RuntimeContext;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/** An OSGi runtime context. */
public class OsgiRuntimeContext implements RuntimeContext, AutoCloseable {
	private final static Logger logger = System.getLogger(OsgiRuntimeContext.class.getName());

	private final static long STOP_FOR_UPDATE_TIMEOUT = 60 * 1000;
	private final static long CLOSE_TIMEOUT = 60 * 1000;

	private final static String SYMBOLIC_NAME_FELIX_SCR = "org.apache.felix.scr";

	private Map<String, String> config;
	private Framework framework;
//	private OsgiBoot osgiBoot;

	/**
	 * Constructor to use when the runtime context will create the OSGi
	 * {@link Framework}.
	 */
	public OsgiRuntimeContext(Map<String, String> config) {
		this.config = config;
	}

	/**
	 * Constructor to use when the OSGi {@link Framework} has been created by other
	 * means.
	 */
	OsgiRuntimeContext(BundleContext bundleContext) {
		start(bundleContext);
	}

	@Override
	public void run() {
		if (framework != null && framework.getState() >= Framework.STARTING)
			throw new IllegalStateException("OSGi framework is already started");

		if (framework == null) {
			ServiceLoader<FrameworkFactory> sl = ServiceLoader.load(FrameworkFactory.class);
			Optional<FrameworkFactory> opt = sl.findFirst();
			if (opt.isEmpty())
				throw new IllegalStateException("Cannot find OSGi framework");
			framework = opt.get().newFramework(config);
		}

		try {
			framework.start();
			BundleContext bundleContext = framework.getBundleContext();
			start(bundleContext);
		} catch (BundleException e) {
			throw new IllegalStateException("Cannot start OSGi framework", e);
		}
	}

	protected void start(BundleContext bundleContext) {
		// preferences
//		SystemRootPreferences systemRootPreferences = ThinPreferencesFactory.getInstance().getSystemRootPreferences();
//		bundleContext.registerService(AbstractPreferences.class, systemRootPreferences, new Hashtable<>());

		// Make sure LoggerFinder has been searched for, since it is lazily loaded
		LoggerFinder loggerFinder = LoggerFinder.getLoggerFinder();

		if (loggerFinder instanceof Consumer<?> && loggerFinder instanceof Supplier<?>) {
			@SuppressWarnings("unchecked")
			Consumer<Map<String, Object>> consumer = (Consumer<Map<String, Object>>) loggerFinder;
			// ThinLoggerFinder.getConfigurationConsumer()
			// ThinLoggerFinder.getLogEntryPublisher()

			@SuppressWarnings("unchecked")
			Supplier<Flow.Publisher<Map<String, Serializable>>> supplier = (Supplier<Flow.Publisher<Map<String, Serializable>>>) loggerFinder;
			// logging
			bundleContext.registerService(Consumer.class, consumer,
					new Hashtable<>(Collections.singletonMap(Constants.SERVICE_PID, "argeo.logging.configuration")));
			bundleContext.registerService(Flow.Publisher.class, supplier.get(),
					new Hashtable<>(Collections.singletonMap(Constants.SERVICE_PID, "argeo.logging.publisher")));
		}
		OsgiBoot osgiBoot = new OsgiBoot(bundleContext);
		String frameworkUuuid = bundleContext.getProperty(Constants.FRAMEWORK_UUID);
		new Thread("OSGi boot framework " + frameworkUuuid) {
			@Override
			public void run() {
				osgiBoot.bootstrap(config);
			}
		}.start();
	}

	public void update() {
		stop();
		try {
			waitForStop(STOP_FOR_UPDATE_TIMEOUT);
		} catch (InterruptedException e) {
			logger.log(Level.TRACE, "Wait for stop interrupted", e);
		}
		run();

		// TODO Optimise with OSGi mechanisms (e.g. framework.update())
//		if (osgiBoot != null) {
//			Objects.requireNonNull(osgiBoot);
//			osgiBoot.update();
//		}
	}

	protected void stop() {
		if (framework == null)
			return;
		stop(framework.getBundleContext());
		try {
			framework.stop();
		} catch (BundleException e) {
			throw new IllegalStateException("Cannot stop OSGi framework", e);
		}
	}

	protected void stop(BundleContext bundleContext) {
//		if (loggingConfigurationSr != null)
//			try {
//				loggingConfigurationSr.unregister();
//			} catch (Exception e) {
//				// silent
//			}
//		if (logEntryPublisherSr != null)
//			try {
//				logEntryPublisherSr.unregister();
//			} catch (Exception e) {
//				// silent
//			}
	}

	@Override
	public void waitForStop(long timeout) throws InterruptedException {
		if (framework == null)
			return;

		framework.waitForStop(timeout);
	}

	public void close() throws Exception {
		if (framework == null)
			return;
		// TODO make shutdown of dynamic service more robust
//		for (Bundle scrBundle : framework.getBundleContext().getBundles()) {
//			if (scrBundle.getSymbolicName().equals(SYMBOLIC_NAME_FELIX_SCR)) {
//				if (scrBundle.getState() > Bundle.RESOLVED) {
//					scrBundle.stop();
//					while (!(scrBundle.getState() <= Bundle.RESOLVED)) {
//						Thread.sleep(100);
//					}
//					Thread.sleep(500);
//				}
//			}
//		}

		stop();
		waitForStop(CLOSE_TIMEOUT);
		framework = null;
//			osgiBoot = null;
		config.clear();
	}

	public Framework getFramework() {
		return framework;
	}

}
