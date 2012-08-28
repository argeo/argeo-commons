package org.argeo.jcr;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;

import org.argeo.ArgeoException;

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
			throw new ArgeoException(
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
			throw new ArgeoException(
					"Unexpected exception when trying to retrieve repository with uri "
							+ uri, e);
		}
	}

	private ArgeoJcrUtils() {
	}

}
