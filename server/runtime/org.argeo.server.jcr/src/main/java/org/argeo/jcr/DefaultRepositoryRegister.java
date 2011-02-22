package org.argeo.jcr;

import java.util.Collections;
import java.util.Map;
import java.util.Observable;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultRepositoryRegister extends Observable implements
		RepositoryRegister {
	private final static Log log = LogFactory
			.getLog(DefaultRepositoryRegister.class);

	private SortedMap<String, Repository> repositories = Collections
			.synchronizedSortedMap(new TreeMap<String, Repository>());

	@SuppressWarnings("rawtypes")
	public synchronized Repository getRepository(Map parameters)
			throws RepositoryException {
		if (!parameters.containsKey(JCR_REPOSITORY_NAME))
			throw new RepositoryException("Parameter " + JCR_REPOSITORY_NAME
					+ " has to be defined.");
		String name = parameters.get(JCR_REPOSITORY_NAME).toString();
		if (!repositories.containsKey(name))
			throw new RepositoryException("No repository registered with name "
					+ name);

		return repositories.get(name);
	}

	public Map<String, Repository> getRepositories() {
		return new TreeMap<String, Repository>(repositories);
	}

	/** Registers a service, typically called when OSGi services are bound. */
	@SuppressWarnings("rawtypes")
	public synchronized void register(Repository repository, Map properties) {
		// TODO: also check bean name?
		if (properties == null || !properties.containsKey(JCR_REPOSITORY_NAME)) {
			log.warn("Cannot register a repository without property "
					+ JCR_REPOSITORY_NAME);
			return;
		}

		String name = properties.get(JCR_REPOSITORY_NAME).toString();
		repositories.put(name, repository);
		setChanged();
		notifyObservers(repository);
	}

	/** Unregisters a service, typically called when OSGi services are unbound. */
	@SuppressWarnings("rawtypes")
	public synchronized void unregister(Repository repository, Map properties) {
		// TODO: also check bean name?
		if (properties == null || !properties.containsKey(JCR_REPOSITORY_NAME)) {
			log.warn("Cannot unregister a repository without property "
					+ JCR_REPOSITORY_NAME);
			return;
		}

		String name = properties.get(JCR_REPOSITORY_NAME).toString();
		repositories.remove(name);
		setChanged();
		notifyObservers(repository);
	}
}
