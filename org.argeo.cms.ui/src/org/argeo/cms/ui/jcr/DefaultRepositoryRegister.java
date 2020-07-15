package org.argeo.cms.ui.jcr;

import java.util.Collections;
import java.util.Map;
import java.util.Observable;
import java.util.TreeMap;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.api.NodeConstants;

public class DefaultRepositoryRegister extends Observable implements RepositoryRegister {
	/** Key for a JCR repository alias */
	private final static String CN = NodeConstants.CN;
	/** Key for a JCR repository URI */
	// public final static String JCR_REPOSITORY_URI = "argeo.jcr.repository.uri";
	private final static Log log = LogFactory.getLog(DefaultRepositoryRegister.class);

	/** Read only map which will be directly exposed. */
	private Map<String, Repository> repositories = Collections.unmodifiableMap(new TreeMap<String, Repository>());

	@SuppressWarnings("rawtypes")
	public synchronized Repository getRepository(Map parameters) throws RepositoryException {
		if (!parameters.containsKey(CN))
			throw new RepositoryException("Parameter " + CN + " has to be defined.");
		String alias = parameters.get(CN).toString();
		if (!repositories.containsKey(alias))
			throw new RepositoryException("No repository registered with alias " + alias);

		return repositories.get(alias);
	}

	/** Access to the read-only map */
	public synchronized Map<String, Repository> getRepositories() {
		return repositories;
	}

	/** Registers a service, typically called when OSGi services are bound. */
	@SuppressWarnings("rawtypes")
	public synchronized void register(Repository repository, Map properties) {
		String alias;
		if (properties == null || !properties.containsKey(CN)) {
			log.warn("Cannot register a repository if no " + CN + " property is specified.");
			return;
		}
		alias = properties.get(CN).toString();
		Map<String, Repository> map = new TreeMap<String, Repository>(repositories);
		map.put(alias, repository);
		repositories = Collections.unmodifiableMap(map);
		setChanged();
		notifyObservers(alias);
	}

	/** Unregisters a service, typically called when OSGi services are unbound. */
	@SuppressWarnings("rawtypes")
	public synchronized void unregister(Repository repository, Map properties) {
		// TODO: also check bean name?
		if (properties == null || !properties.containsKey(CN)) {
			log.warn("Cannot unregister a repository without property " + CN);
			return;
		}

		String alias = properties.get(CN).toString();
		Map<String, Repository> map = new TreeMap<String, Repository>(repositories);
		if (map.remove(alias) == null) {
			log.warn("No repository was registered with alias " + alias);
			return;
		}
		repositories = Collections.unmodifiableMap(map);
		setChanged();
		notifyObservers(alias);
	}
}
