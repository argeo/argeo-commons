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

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;

/** Utilities related to Argeo model in JCR */
public class ArgeoJcrUtils implements ArgeoJcrConstants {
	/**
	 * Wraps the call to the repository factory based on parameter
	 * {@link ArgeoJcrConstants#JCR_REPOSITORY_ALIAS} in order to simplify it
	 * and protect against future API changes.
	 */
	public static Repository getRepositoryByAlias(
			RepositoryFactory repositoryFactory, String alias) {
		try {
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(JCR_REPOSITORY_ALIAS, alias);
			return repositoryFactory.getRepository(parameters);
		} catch (RepositoryException e) {
			throw new ArgeoJcrException(
					"Unexpected exception when trying to retrieve repository with alias "
							+ alias, e);
		}
	}

	/**
	 * Wraps the call to the repository factory based on parameter
	 * {@link ArgeoJcrConstants#JCR_REPOSITORY_URI} in order to simplify it and
	 * protect against future API changes.
	 */
	public static Repository getRepositoryByUri(
			RepositoryFactory repositoryFactory, String uri) {
		return getRepositoryByUri(repositoryFactory, uri, null);
	}

	/**
	 * Wraps the call to the repository factory based on parameter
	 * {@link ArgeoJcrConstants#JCR_REPOSITORY_URI} in order to simplify it and
	 * protect against future API changes.
	 */
	public static Repository getRepositoryByUri(
			RepositoryFactory repositoryFactory, String uri, String alias) {
		try {
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(JCR_REPOSITORY_URI, uri);
			if (alias != null)
				parameters.put(JCR_REPOSITORY_ALIAS, alias);
			return repositoryFactory.getRepository(parameters);
		} catch (RepositoryException e) {
			throw new ArgeoJcrException(
					"Unexpected exception when trying to retrieve repository with uri "
							+ uri, e);
		}
	}

	private ArgeoJcrUtils() {
	}

}
