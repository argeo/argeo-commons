package org.argeo.cms.internal.kernel;

import javax.jcr.RepositoryFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

	private final BundleContext bundleContext;

	private JackrabbitNode node;
	private OsgiJackrabbitRepositoryFactory repositoryFactory;
	private NodeSecurity nodeSecurity;
	private NodeHttpFilter httpFilter;

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
			httpFilter = new NodeHttpFilter(bundleContext, nodeSecurity);

			// Publish services to OSGi register
			nodeSecurity.publish();
			node.publish();
			bundleContext.registerService(RepositoryFactory.class,
					repositoryFactory, null);
			httpFilter.publish();
		} catch (Exception e) {
			log.error("Cannot initialize Argeo CMS", e);
			throw new ArgeoException("Cannot initialize", e);
		}

		long duration = System.currentTimeMillis() - begin;
		log.info("## ARGEO CMS UP in " + (duration / 1000) + "."
				+ (duration % 1000) + "s ##");
	}

	void destroy() {
		long begin = System.currentTimeMillis();

		httpFilter = null;
		nodeSecurity.destroy();
		node.destroy();

		long duration = System.currentTimeMillis() - begin;
		log.info("## ARGEO CMS DOWN in " + (duration / 1000) + "."
				+ (duration % 1000) + "s ##");
	}

}
