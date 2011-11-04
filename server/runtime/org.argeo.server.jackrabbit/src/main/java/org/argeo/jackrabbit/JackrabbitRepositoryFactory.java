package org.argeo.jackrabbit;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.jcr2dav.Jcr2davRepositoryFactory;
import org.argeo.ArgeoException;
import org.argeo.jcr.ArgeoJcrConstants;
import org.argeo.jcr.DefaultRepositoryFactory;
import org.osgi.framework.BundleContext;

/** Repository factory which can access remote Jackrabbit repositories */
public class JackrabbitRepositoryFactory extends DefaultRepositoryFactory
		implements RepositoryFactory, ArgeoJcrConstants {
	private final static Log log = LogFactory
			.getLog(JackrabbitRepositoryFactory.class);

	private BundleContext bundleContext;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Repository getRepository(Map parameters) throws RepositoryException {
		Repository repository = super.getRepository(parameters);
		if (repository != null)
			return repository;

		String uri;
		if (parameters.containsKey(JCR_REPOSITORY_URI))
			uri = parameters.get(JCR_REPOSITORY_URI).toString();
		else if (parameters.containsKey(JcrUtils.REPOSITORY_URI))
			uri = parameters.get(JcrUtils.REPOSITORY_URI).toString();
		else
			return null;

		Map<String, String> params = new HashMap<String, String>();
		params.put(JcrUtils.REPOSITORY_URI, uri);
		repository = new Jcr2davRepositoryFactory().getRepository(params);
		if (repository == null)
			throw new ArgeoException("Remote Davex repository " + uri
					+ " not found");
		log.info("Initialized remote Jackrabbit repository from uri " + uri);

		if (parameters.containsKey(JCR_REPOSITORY_ALIAS)
				&& bundleContext != null) {
			Properties properties = new Properties();
			properties.putAll(parameters);
			bundleContext.registerService(Repository.class.getName(),
					repository, properties);
			log.info("Registered under alias '"
					+ parameters.get(JCR_REPOSITORY_ALIAS)
					+ "' the remote JCR repository from uri " + uri);
		}

		return repository;
	}

	public void setBundleContext(BundleContext bundleContext) {
		this.bundleContext = bundleContext;
	}

}
