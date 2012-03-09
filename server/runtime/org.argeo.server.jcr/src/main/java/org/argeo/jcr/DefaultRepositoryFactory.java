/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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

import java.util.Map;
import java.util.Properties;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;

public class DefaultRepositoryFactory extends DefaultRepositoryRegister
		implements RepositoryFactory, ArgeoJcrConstants {
	// private final static Log log = LogFactory
	// .getLog(DefaultRepositoryFactory.class);

	@SuppressWarnings("rawtypes")
	public Repository getRepository(Map parameters) throws RepositoryException {
		if (parameters.containsKey(JCR_REPOSITORY_ALIAS)) {
			String alias = parameters.get(JCR_REPOSITORY_ALIAS).toString();
			return getRepositoryByAlias(alias);
		}
		return null;
	}

	/**
	 * Retrieve a repository by alias
	 * 
	 * @return the repository registered with alias or null if none
	 */
	protected Repository getRepositoryByAlias(String alias) {
		if (getRepositories().containsKey(alias))
			return getRepositories().get(alias);
		else
			return null;
	}

	protected void publish(String alias, Repository repository,
			Properties properties) {
		register(repository, properties);
	}

}
