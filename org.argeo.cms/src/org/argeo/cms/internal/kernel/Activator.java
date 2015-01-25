package org.argeo.cms.internal.kernel;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class Activator implements BundleActivator {
	private Kernel kernel;

	@Override
	public void start(BundleContext context) throws Exception {
		assert kernel == null;
		kernel = new Kernel(context);
		kernel.init();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		kernel.destroy();
		kernel = null;
	}

}
