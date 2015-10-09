/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.jcr;

import java.util.Collections;
import java.util.Map;
import java.util.Observable;
import java.util.TreeMap;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class DefaultRepositoryRegister extends Observable implements
		RepositoryRegister, ArgeoJcrConstants {
	private final static Log log = LogFactory
			.getLog(DefaultRepositoryRegister.class);

	/** Read only map which will be directly exposed. */
	private Map<String, Repository> repositories = Collections
			.unmodifiableMap(new TreeMap<String, Repository>());

	@SuppressWarnings("rawtypes")
	public synchronized Repository getRepository(Map parameters)
			throws RepositoryException {
		if (!parameters.containsKey(JCR_REPOSITORY_ALIAS))
			throw new RepositoryException("Parameter " + JCR_REPOSITORY_ALIAS
					+ " has to be defined.");
		String alias = parameters.get(JCR_REPOSITORY_ALIAS).toString();
		if (!repositories.containsKey(alias))
			throw new RepositoryException(
					"No repository registered with alias " + alias);

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
		if (properties == null || !properties.containsKey(JCR_REPOSITORY_ALIAS)) {
			log.warn("Cannot register a repository if no "
					+ JCR_REPOSITORY_ALIAS + " property is speecified.");
			return;
		}
		alias = properties.get(JCR_REPOSITORY_ALIAS).toString();
		Map<String, Repository> map = new TreeMap<String, Repository>(
				repositories);
		map.put(alias, repository);
		repositories = Collections.unmodifiableMap(map);
		setChanged();
		notifyObservers(alias);
	}

	/** Unregisters a service, typically called when OSGi services are unbound. */
	@SuppressWarnings("rawtypes")
	public synchronized void unregister(Repository repository, Map properties) {
		// TODO: also check bean name?
		if (properties == null || !properties.containsKey(JCR_REPOSITORY_ALIAS)) {
			log.warn("Cannot unregister a repository without property "
					+ JCR_REPOSITORY_ALIAS);
			return;
		}

		String alias = properties.get(JCR_REPOSITORY_ALIAS).toString();
		Map<String, Repository> map = new TreeMap<String, Repository>(
				repositories);
		if (map.remove(alias) == null) {
			log.warn("No repository was registered with alias " + alias);
			return;
		}
		repositories = Collections.unmodifiableMap(map);
		setChanged();
		notifyObservers(alias);
	}
}
