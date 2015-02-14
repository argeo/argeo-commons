package org.argeo.cms.internal.kernel;

import java.util.UUID;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activates the {@link Kernel} from the provided {@link BundleContext}. Gives
 * access to kernel information for the rest of the bundle (and only it)
 */
public class Activator implements BundleActivator {
	private final static String systemKey = UUID.randomUUID().toString();

	private static BundleContext bundleContext;
	private Kernel kernel;

	@Override
	public void start(BundleContext context) throws Exception {
		assert bundleContext == null;
		assert kernel == null;

		bundleContext = context;
		kernel = new Kernel(bundleContext);
		kernel.init();
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		kernel.destroy();
		kernel = null;
		bundleContext = null;
	}

	/**
	 * Singleton interface to the {@link BundleContext} related to the calling
	 * thread. Can be used only within the CMS bundle.
	 */
	public static BundleContext getBundleContext() {
		return bundleContext;
	}

	/**
	 * @return a String which is guaranteed to be unique between and constant
	 *         within a Java static context (typically a VM launch)
	 */
	public final static String getSystemKey() {
		return systemKey;
	}

}
