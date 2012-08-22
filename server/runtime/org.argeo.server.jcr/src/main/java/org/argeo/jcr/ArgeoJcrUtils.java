package org.argeo.jcr;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;

import org.argeo.ArgeoException;

/** Utilities related to Argeo model in JCR */
public class ArgeoJcrUtils implements ArgeoJcrConstants {
	/**
	 * Returns the home node of the session user or null if none was found.
	 * 
	 * @param session
	 *            the session to use in order to perform the search, this can be
	 *            a session with a different user ID than the one searched,
	 *            typically when a system or admin session is used.
	 * @param username
	 *            the username of the user
	 */
	public static Node getUserHome(Session session, String username) {
		try {
			String homePath = ArgeoJcrUtils.getUserHomePath(username);
			return session.itemExists(homePath) ? session.getNode(homePath)
					: null;
			// kept for example of QOM queries
			// QueryObjectModelFactory qomf = session.getWorkspace()
			// .getQueryManager().getQOMFactory();
			// Selector userHomeSel = qomf.selector(ArgeoTypes.ARGEO_USER_HOME,
			// "userHome");
			// DynamicOperand userIdDop = qomf.propertyValue("userHome",
			// ArgeoNames.ARGEO_USER_ID);
			// StaticOperand userIdSop = qomf.literal(session.getValueFactory()
			// .createValue(username));
			// Constraint constraint = qomf.comparison(userIdDop,
			// QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO, userIdSop);
			// Query query = qomf.createQuery(userHomeSel, constraint, null,
			// null);
			// Node userHome = JcrUtils.querySingleNode(query);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot find home for user " + username, e);
		}
	}

	/** Returns the home node of the session user or null if none was found. */
	public static Node getUserHome(Session session) {
		String userID = session.getUserID();
		return getUserHome(session, userID);
	}

	/** @deprecated Use {@link #getUserHome(Session, String)} directly */
	@Deprecated
	public static String getUserHomePath(String username) {
		String homeBasePath = DEFAULT_HOME_BASE_PATH;
		return homeBasePath + '/' + JcrUtils.firstCharsToPath(username, 2)
				+ '/' + username;
	}

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
		try {
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(JCR_REPOSITORY_URI, uri);
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
