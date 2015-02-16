package org.argeo.cms.internal.kernel;

import java.lang.management.ManagementFactory;

import javax.jcr.RepositoryFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.util.TransientFileFactory;
import org.argeo.ArgeoException;
import org.argeo.jackrabbit.OsgiJackrabbitRepositoryFactory;
import org.argeo.security.core.InternalAuthentication;
import org.osgi.framework.BundleContext;
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
	// private static final String PROP_WORKBENCH_AUTOSTART =
	// "org.eclipse.rap.workbenchAutostart";

	private final BundleContext bundleContext;

	private JackrabbitNode node;
	private RepositoryFactory repositoryFactory;
	private NodeSecurity nodeSecurity;
	private NodeHttp nodeHttp;

	Kernel(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	void init() {
		ClassLoader currentContextCl = Thread.currentThread()
				.getContextClassLoader();
		// We use the CMS bundle classloader during initialization
		Thread.currentThread().setContextClassLoader(
				Kernel.class.getClassLoader());

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
		} catch (Exception e) {
			log.error("Cannot initialize Argeo CMS", e);
			throw new ArgeoException("Cannot initialize", e);
		} finally {
			Thread.currentThread().setContextClassLoader(currentContextCl);
		}

		long jvmUptime = ManagementFactory.getRuntimeMXBean().getUptime();
		log.info("## ARGEO CMS UP in " + (jvmUptime / 1000) + "."
				+ (jvmUptime % 1000) + "s ##");
		long initDuration = System.currentTimeMillis() - begin;
		if (log.isTraceEnabled())
			log.trace("Kernel initialization took " + initDuration + "ms");
		directorsCut(initDuration);
	}

	void destroy() {
		long begin = System.currentTimeMillis();

		nodeHttp = null;
		nodeSecurity.destroy();
		node.destroy();

		// Clean hanging threads from Jackrabbit
		TransientFileFactory.shutdown();

		long duration = System.currentTimeMillis() - begin;
		log.info("## ARGEO CMS DOWN in " + (duration / 1000) + "."
				+ (duration % 1000) + "s ##");
	}

	private void directorsCut(long initDuration) {
		// final long ms = 128l + (long) (Math.random() * 128d);
		long ms = initDuration / 10;
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
