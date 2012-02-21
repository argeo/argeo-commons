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

/**
 * Repository factory which can create new repositories and access remote
 * Jackrabbit repositories
 */
public class JackrabbitRepositoryFactory extends DefaultRepositoryFactory
		implements RepositoryFactory, ArgeoJcrConstants {
	private final static Log log = LogFactory
			.getLog(JackrabbitRepositoryFactory.class);

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public Repository getRepository(Map parameters) throws RepositoryException {
		// check if can be found by alias
		Repository repository = super.getRepository(parameters);
		if (repository != null)
			return repository;

		// check if remote
		String uri = null;
		if (parameters.containsKey(JCR_REPOSITORY_URI))
			uri = parameters.get(JCR_REPOSITORY_URI).toString();
		else if (parameters.containsKey(JcrUtils.REPOSITORY_URI))
			uri = parameters.get(JcrUtils.REPOSITORY_URI).toString();

		if (uri != null)
			repository = createRemoteRepository(uri);

		if (parameters.containsKey(JCR_REPOSITORY_ALIAS)) {
			Properties properties = new Properties();
			properties.putAll(parameters);
			String alias = parameters.get(JCR_REPOSITORY_ALIAS).toString();
			publish(alias, repository, properties);
			log.info("Registered JCR repository under alias '" + alias
					+ "' with properties " + properties);
		}

		return repository;
	}

	protected Repository createRemoteRepository(String uri)
			throws RepositoryException {
		Map<String, String> params = new HashMap<String, String>();
		params.put(JcrUtils.REPOSITORY_URI, uri);
		Repository repository = new Jcr2davRepositoryFactory()
				.getRepository(params);
		if (repository == null)
			throw new ArgeoException("Remote Davex repository " + uri
					+ " not found");
		log.info("Initialized remote Jackrabbit repository from uri " + uri);
		return repository;
	}

	/**
	 * Called after the repository has been initialized. Does nothing by
	 * default.
	 */
	@SuppressWarnings("rawtypes")
	protected void postInitialization(Repository repository, Map parameters) {

	}

}
