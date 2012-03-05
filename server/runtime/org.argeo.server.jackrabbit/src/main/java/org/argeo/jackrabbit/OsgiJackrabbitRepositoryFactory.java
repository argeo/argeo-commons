package org.argeo.jackrabbit;

import java.util.Properties;

import javax.jcr.Repository;

import org.osgi.framework.BundleContext;

/**
 * OSGi-aware Jackrabbit repository factory which can retrieve/publish
 * {@link Repository} as OSGi services.
 */
public class OsgiJackrabbitRepositoryFactory extends
		JackrabbitRepositoryFactory {
//	private final static Log log = LogFactory
//			.getLog(OsgiJackrabbitRepositoryFactory.class);
	private BundleContext bundleContext;

	protected void publish(String alias, Repository repository,
			Properties properties) {
		if (bundleContext != null) {
			// do not modify reference
			Properties props = new Properties(properties);
			props.setProperty(JCR_REPOSITORY_ALIAS, alias);
			bundleContext.registerService(Repository.class.getName(),
					repository, props);
		}
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

}
