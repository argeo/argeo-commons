package org.argeo.init.osgi;

import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

import org.argeo.init.RuntimeContext;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

public class OsgiRuntimeContext implements RuntimeContext {
	private Map<String, String> config;
	private Framework framework;
	private OsgiBoot osgiBoot;

	public OsgiRuntimeContext(Map<String, String> config) {
		this.config = config;
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
			osgiBoot = new OsgiBoot(bundleContext);
			osgiBoot.bootstrap();
		} catch (BundleException e) {
			throw new IllegalStateException("Cannot start OSGi framework", e);
		}
	}

	@Override
	public void waitForStop(long timeout) throws InterruptedException {
		if (framework == null)
			throw new IllegalStateException("Framework is not initialised");
		framework.waitForStop(timeout);
	}

	@Override
	public void close() throws Exception {
		if (framework != null)
			framework.stop();
	}

}
