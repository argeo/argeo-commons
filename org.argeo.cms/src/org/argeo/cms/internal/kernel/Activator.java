package org.argeo.cms.internal.kernel;

import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activates the {@link Kernel} from the provided {@link BundleContext}. Gives
 * access to kernel information for the rest of the bundle (and only it)
 */
public class Activator implements BundleActivator {
	public final static String SYSTEM_KEY_PROPERTY = "argeo.security.systemKey";
	private final Log log = LogFactory.getLog(Activator.class);

	private final static String systemKey;
	static {
		systemKey = UUID.randomUUID().toString();
		System.setProperty(SYSTEM_KEY_PROPERTY, systemKey);
	}

	private static BundleContext bundleContext;
	private Kernel kernel;

	@Override
	public void start(BundleContext context) throws Exception {
		assert bundleContext == null;
		assert kernel == null;
		bundleContext = context;
		try {
			kernel = new Kernel();
			kernel.init();
		} catch (Exception e) {
			log.error("Cannot boot kernel", e);
		}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		kernel.destroy();
		kernel = null;
		bundleContext = null;
	}

	/**
	 * Singleton interface to the {@link BundleContext} related to the calling
	 * thread.
	 * 
	 * @BundleScope
	 */
	public static BundleContext getBundleContext() {
		return bundleContext;
	}

	/**
	 * @return a String which is guaranteed to be unique between and constant
	 *         within a Java static context (typically a VM launch)
	 * @BundleScope
	 */
	public final static String getSystemKey() {
		return systemKey;
	}
}
