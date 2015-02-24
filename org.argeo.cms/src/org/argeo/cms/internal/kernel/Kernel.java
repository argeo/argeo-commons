package org.argeo.cms.internal.kernel;

import java.lang.management.ManagementFactory;

import javax.jcr.Repository;
import javax.jcr.RepositoryFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.util.TransientFileFactory;
import org.argeo.ArgeoException;
import org.argeo.cms.CmsException;
import org.argeo.jackrabbit.OsgiJackrabbitRepositoryFactory;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.security.core.InternalAuthentication;
import org.eclipse.equinox.http.servlet.ExtendedHttpService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
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
final class Kernel implements ServiceListener {
	private final static Log log = LogFactory.getLog(Kernel.class);

	private final BundleContext bundleContext;
	// private final ThreadGroup threadGroup = new
	// ThreadGroup("Argeo CMS Kernel");

	private JackrabbitNode node;
	private RepositoryFactory repositoryFactory;
	private NodeSecurity nodeSecurity;
	private NodeHttp nodeHttp;

	Kernel(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

	void init() {
		// new Thread(threadGroup, "init") {
		// @Override
		// public void run() {
		// // CMS bundle classloader used during initialisation
		// Thread.currentThread().setContextClassLoader(
		// Kernel.class.getClassLoader());
		doInit();
		// }
		// }.start();
	}

	protected void doInit() {
		ClassLoader currentContextCl = Thread.currentThread()
				.getContextClassLoader();
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

			// Equinox dependency
			ExtendedHttpService httpService = waitForHttpService();
			nodeHttp = new NodeHttp(httpService, node, nodeSecurity);

			// Publish services to OSGi
			nodeSecurity.publish();
			node.publish();
			bundleContext.registerService(RepositoryFactory.class,
					repositoryFactory, null);

			bundleContext.addServiceListener(Kernel.this);
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

		if (nodeHttp != null)
			nodeHttp.destroy();
		if (nodeSecurity != null)
			nodeSecurity.destroy();
		if (node != null)
			node.destroy();

		bundleContext.removeServiceListener(this);

		// Clean hanging threads from Jackrabbit
		TransientFileFactory.shutdown();

		long duration = System.currentTimeMillis() - begin;
		log.info("## ARGEO CMS DOWN in " + (duration / 1000) + "."
				+ (duration % 1000) + "s ##");
	}

	@Override
	public void serviceChanged(ServiceEvent event) {
		ServiceReference<?> sr = event.getServiceReference();
		Object jcrRepoAlias = sr
				.getProperty(ArgeoJcrConstants.JCR_REPOSITORY_ALIAS);
		if (jcrRepoAlias != null) {// JCR repository
			String alias = jcrRepoAlias.toString();
			Repository repository = (Repository) bundleContext.getService(sr);
			if (ServiceEvent.REGISTERED == event.getType()) {
				try {
					nodeHttp.registerWebdavServlet(alias, repository, true);
					nodeHttp.registerWebdavServlet(alias, repository, false);
					nodeHttp.registerRemotingServlet(alias, repository, true);
					nodeHttp.registerRemotingServlet(alias, repository, false);
				} catch (Exception e) {
					throw new CmsException("Could not publish JCR repository "
							+ alias, e);
				}
			} else if (ServiceEvent.UNREGISTERING == event.getType()) {
			}
		}

	}

	final private static void directorsCut(long initDuration) {
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
		if (log.isTraceEnabled())
			log.trace("Sleep accuracy: "
					+ String.format("%.2f", sleepAccuracy * 100) + " %");
	}

	private ExtendedHttpService waitForHttpService() {
		final ServiceTracker<ExtendedHttpService, ExtendedHttpService> st = new ServiceTracker<ExtendedHttpService, ExtendedHttpService>(
				bundleContext, ExtendedHttpService.class, null);
		st.open();
		ExtendedHttpService httpService;
		try {
			httpService = st.waitForService(1000);
		} catch (InterruptedException e) {
			httpService = null;
		}

		if (httpService == null)
			throw new CmsException("Could not find "
					+ ExtendedHttpService.class + " service.");
		return httpService;
	}
}
