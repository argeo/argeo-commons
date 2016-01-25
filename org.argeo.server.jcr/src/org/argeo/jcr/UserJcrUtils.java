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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.StaticOperand;

import org.argeo.ArgeoException;

/**
 * Utilities related to the user home and properties based on Argeo JCR model.
 * Do not use anymore. Does not fit with current security model
 */
@Deprecated
public class UserJcrUtils {
	/** The home base path. Not yet configurable */
	public final static String DEFAULT_HOME_BASE_PATH = "/home";

	/**
	 * Returns the home node of the user or null if none was found.
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
			// String homePath = UserJcrUtils.getUserHomePath(username);
			// return session.itemExists(homePath) ? session.getNode(homePath)
			// : null;
			// kept for example of QOM queries
			QueryObjectModelFactory qomf = session.getWorkspace()
					.getQueryManager().getQOMFactory();
			Selector userHomeSel = qomf.selector(ArgeoTypes.ARGEO_USER_HOME,
					"userHome");
			DynamicOperand userIdDop = qomf.propertyValue(
					userHomeSel.getSelectorName(), ArgeoNames.ARGEO_USER_ID);
			StaticOperand userIdSop = qomf.literal(session.getValueFactory()
					.createValue(username));
			Constraint constraint = qomf.comparison(userIdDop,
					QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO, userIdSop);
			Query query = qomf.createQuery(userHomeSel, constraint, null, null);
			return JcrUtils.querySingleNode(query);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot find home for user " + username, e);
		}
	}

	public static Node getUserProfile(Session session, String username) {
		try {
			QueryObjectModelFactory qomf = session.getWorkspace()
					.getQueryManager().getQOMFactory();
			Selector userHomeSel = qomf.selector(ArgeoTypes.ARGEO_USER_PROFILE,
					"userProfile");
			DynamicOperand userIdDop = qomf.propertyValue(
					userHomeSel.getSelectorName(), ArgeoNames.ARGEO_USER_ID);
			StaticOperand userIdSop = qomf.literal(session.getValueFactory()
					.createValue(username));
			Constraint constraint = qomf.comparison(userIdDop,
					QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO, userIdSop);
			Query query = qomf.createQuery(userHomeSel, constraint, null, null);
			return JcrUtils.querySingleNode(query);
		} catch (RepositoryException e) {
			throw new ArgeoException(
					"Cannot find profile for user " + username, e);
		}
	}

	/** Returns the home node of the session user or null if none was found. */
	public static Node getUserHome(Session session) {
		String userID = session.getUserID();
		return getUserHome(session, userID);
	}

	private UserJcrUtils() {
	}
}
