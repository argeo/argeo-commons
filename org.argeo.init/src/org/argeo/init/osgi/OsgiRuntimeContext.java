package org.argeo.init.osgi;

import java.lang.System.LoggerFinder;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.concurrent.Flow;
import java.util.function.Consumer;

import org.argeo.init.RuntimeContext;
import org.argeo.init.logging.ThinLoggerFinder;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/** An OSGi runtime context. */
public class OsgiRuntimeContext implements RuntimeContext, AutoCloseable {
	private Map<String, String> config;
	private Framework framework;
	private OsgiBoot osgiBoot;

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
		ServiceLoader<FrameworkFactory> sl = ServiceLoader.load(FrameworkFactory.class);
		Optional<FrameworkFactory> opt = sl.findFirst();
		if (opt.isEmpty())
			throw new IllegalStateException("Cannot find OSGi framework");
		framework = opt.get().newFramework(config);
		try {
			framework.start();
			BundleContext bundleContext = framework.getBundleContext();
			start(bundleContext);
		} catch (BundleException e) {
			throw new IllegalStateException("Cannot start OSGi framework", e);
		}
	}

	public void start(BundleContext bundleContext) {
		// preferences
//		SystemRootPreferences systemRootPreferences = ThinPreferencesFactory.getInstance().getSystemRootPreferences();
//		bundleContext.registerService(AbstractPreferences.class, systemRootPreferences, new Hashtable<>());

		// Make sure LoggerFinder has been searched for, since it is lazily loaded
		LoggerFinder.getLoggerFinder();

		// logging
		bundleContext.registerService(Consumer.class, ThinLoggerFinder.getConfigurationConsumer(),
				new Hashtable<>(Collections.singletonMap(Constants.SERVICE_PID, "argeo.logging.configuration")));
		bundleContext.registerService(Flow.Publisher.class, ThinLoggerFinder.getLogEntryPublisher(),
				new Hashtable<>(Collections.singletonMap(Constants.SERVICE_PID, "argeo.logging.publisher")));

		osgiBoot = new OsgiBoot(bundleContext);
		osgiBoot.bootstrap(config);

	}

	public void update() {
		Objects.requireNonNull(osgiBoot);
		osgiBoot.update();
	}

	public void stop(BundleContext bundleContext) {
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
			throw new IllegalStateException("Framework is not initialised");

		framework.waitForStop(timeout);
	}

	public void close() throws Exception {
		// TODO make shutdown of dynamic service more robust
		Bundle scrBundle = osgiBoot.getBundlesBySymbolicName().get("org.apache.felix.scr");
		if (scrBundle != null) {
			scrBundle.stop();
			while (!(scrBundle.getState() <= Bundle.RESOLVED)) {
				Thread.sleep(500);
			}
			Thread.sleep(1000);
		}

		stop(framework.getBundleContext());
		if (framework != null)
			framework.stop();

	}

	public Framework getFramework() {
		return framework;
	}

}
