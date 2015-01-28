package org.argeo.cms.internal.kernel;

import javax.jcr.RepositoryFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.argeo.jackrabbit.OsgiJackrabbitRepositoryFactory;
import org.argeo.security.core.InternalAuthentication;
import org.eclipse.rap.rwt.application.ApplicationConfiguration;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Argeo CMS Kernel. Responsible for :
 * <ul>
 * <li>security</li>
 * <li>provisioning</li>
 * <li>transaction</li>
 * <li>logging</li>
 * <li>local and remote file systems access</li>
 * <li>OS access</li>
 * </ul>
 */
final class Kernel {
	private final static Log log = LogFactory.getLog(Kernel.class);
//	private static final String PROP_WORKBENCH_AUTOSTART = "org.eclipse.rap.workbenchAutostart";

	private final BundleContext bundleContext;

	private JackrabbitNode node;
	private RepositoryFactory repositoryFactory;
	private NodeSecurity nodeSecurity;
	private NodeHttp nodeHttp;

	private ServiceRegistration<ApplicationConfiguration> workbenchReg;

	Kernel(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	void init() {
		long begin = System.currentTimeMillis();
		InternalAuthentication initAuth = new InternalAuthentication(
				KernelConstants.DEFAULT_SECURITY_KEY);
		SecurityContextHolder.getContext().setAuthentication(initAuth);

		try {
			node = new JackrabbitNode(bundleContext);
			repositoryFactory = new OsgiJackrabbitRepositoryFactory();
			nodeSecurity = new NodeSecurity(bundleContext, node);
			nodeHttp = new NodeHttp(bundleContext, node, nodeSecurity);

			// Publish services to OSGi
			nodeSecurity.publish();
			node.publish();
			bundleContext.registerService(RepositoryFactory.class,
					repositoryFactory, null);
			nodeHttp.publish();

//			if ("false".equals(bundleContext
//					.getProperty(PROP_WORKBENCH_AUTOSTART))) {
//				WorkbenchApplicationConfiguration wac = new WorkbenchApplicationConfiguration();
//				registerWorkbench(wac);
//			}
		} catch (Exception e) {
			log.error("Cannot initialize Argeo CMS", e);
			throw new ArgeoException("Cannot initialize", e);
		}

		long duration = System.currentTimeMillis() - begin;
		log.info("## ARGEO CMS UP in " + (duration / 1000) + "."
				+ (duration % 1000) + "s ##");
		directorsCut();
	}

	void destroy() {
		long begin = System.currentTimeMillis();

		// OSGi
		workbenchReg.unregister();

		nodeHttp = null;
		nodeSecurity.destroy();
		node.destroy();

		long duration = System.currentTimeMillis() - begin;
		log.info("## ARGEO CMS DOWN in " + (duration / 1000) + "."
				+ (duration % 1000) + "s ##");
	}

//	private void registerWorkbench(final WorkbenchApplicationConfiguration wac) {
//		new Thread("Worbench Launcher") {
//			public void run() {
//				Hashtable<String, String> props = new Hashtable<String, String>();
//				props.put(ApplicationLauncher.PROPERTY_CONTEXT_NAME, "ui");
//				workbenchReg = bundleContext.registerService(
//						ApplicationConfiguration.class, wac, props);
//			}
//		}.start();
//	}

	private void directorsCut() {
		final long ms = 128l + (long) (Math.random() * 128d);
		log.info("Spend " + ms + "ms"
				+ " reflecting on the progress brought to mankind"
				+ " by Free Software...");
		long beginNano = System.nanoTime();
		try {
			Thread.sleep(ms, 0);
		} catch (InterruptedException e) {
			// silent
		}
		long durationNano = System.nanoTime() - beginNano;
		final double M = 1000d * 1000d;
		double sleepAccuracy = ((double) durationNano) / (ms * M);
		if (log.isDebugEnabled())
			log.debug("Sleep accuracy: "
					+ String.format("%.2f", sleepAccuracy * 100) + " %");
	}

}
