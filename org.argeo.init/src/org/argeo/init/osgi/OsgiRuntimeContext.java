package org.argeo.init.osgi;

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
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

/** An OSGi runtime context. */
public class OsgiRuntimeContext implements RuntimeContext {
	private Map<String, String> config;
	private Framework framework;
	private OsgiBoot osgiBoot;

	@SuppressWarnings("rawtypes")
	private ServiceRegistration<Consumer> loggingConfigurationSr;
	@SuppressWarnings("rawtypes")
	private ServiceRegistration<Flow.Publisher> logEntryPublisherSr;

	public OsgiRuntimeContext(Map<String, String> config) {
		this.config = config;
	}

	public OsgiRuntimeContext(BundleContext bundleContext) {
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
		// logging
		loggingConfigurationSr = bundleContext.registerService(Consumer.class,
				ThinLoggerFinder.getConfigurationConsumer(),
				new Hashtable<>(Collections.singletonMap(Constants.SERVICE_PID, "argeo.logging.configuration")));
		logEntryPublisherSr = bundleContext.registerService(Flow.Publisher.class,
				ThinLoggerFinder.getLogEntryPublisher(),
				new Hashtable<>(Collections.singletonMap(Constants.SERVICE_PID, "argeo.logging.publisher")));

		osgiBoot = new OsgiBoot(bundleContext);
		osgiBoot.bootstrap();

	}

	public void update() {
		Objects.requireNonNull(osgiBoot);
		osgiBoot.update();
	}

	public void stop(BundleContext bundleContext) {
		if (loggingConfigurationSr != null)
			loggingConfigurationSr.unregister();
		if (logEntryPublisherSr != null)
			logEntryPublisherSr.unregister();

	}

	@Override
	public void waitForStop(long timeout) throws InterruptedException {
		if (framework == null)
			throw new IllegalStateException("Framework is not initialised");
		stop(framework.getBundleContext());
		framework.waitForStop(timeout);
	}

	@Override
	public void close() throws Exception {
		if (framework != null)
			framework.stop();
	}

}
