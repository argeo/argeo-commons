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
package org.argeo.node;

import java.util.HashMap;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.StaticOperand;

/** Utilities related to Argeo model in JCR */
public class NodeUtils {
	/**
	 * Wraps the call to the repository factory based on parameter
	 * {@link NodeConstants#JCR_REPOSITORY_ALIAS} in order to simplify it and
	 * protect against future API changes.
	 */
	public static Repository getRepositoryByAlias(RepositoryFactory repositoryFactory, String alias) {
		try {
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(NodeConstants.CN, alias);
			return repositoryFactory.getRepository(parameters);
		} catch (RepositoryException e) {
			throw new RuntimeException("Unexpected exception when trying to retrieve repository with alias " + alias,
					e);
		}
	}

	/**
	 * Wraps the call to the repository factory based on parameter
	 * {@link NodeConstants#JCR_REPOSITORY_URI} in order to simplify it and
	 * protect against future API changes.
	 */
	public static Repository getRepositoryByUri(RepositoryFactory repositoryFactory, String uri) {
		return getRepositoryByUri(repositoryFactory, uri, null);
	}

	/**
	 * Wraps the call to the repository factory based on parameter
	 * {@link NodeConstants#JCR_REPOSITORY_URI} in order to simplify it and
	 * protect against future API changes.
	 */
	public static Repository getRepositoryByUri(RepositoryFactory repositoryFactory, String uri, String alias) {
		try {
			Map<String, String> parameters = new HashMap<String, String>();
			parameters.put(NodeConstants.LABELED_URI, uri);
			if (alias != null)
				parameters.put(NodeConstants.CN, alias);
			return repositoryFactory.getRepository(parameters);
		} catch (RepositoryException e) {
			throw new RuntimeException("Unexpected exception when trying to retrieve repository with uri " + uri, e);
		}
	}

	private NodeUtils() {
	}

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
			QueryObjectModelFactory qomf = session.getWorkspace().getQueryManager().getQOMFactory();
			Selector sel = qomf.selector(NodeTypes.NODE_USER_HOME, "sel");
			DynamicOperand dop = qomf.propertyValue(sel.getSelectorName(), NodeNames.LDAP_UID);
			StaticOperand sop = qomf.literal(session.getValueFactory().createValue(username));
			Constraint constraint = qomf.comparison(dop, QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO, sop);
			Query query = qomf.createQuery(sel, constraint, null, null);
			return querySingleNode(query);
		} catch (RepositoryException e) {
			throw new RuntimeException("Cannot find home for user " + username, e);
		}
	}

	/**
	 * Returns the home node of the user or null if none was found.
	 * 
	 * @param session
	 *            the session to use in order to perform the search, this can be
	 *            a session with a different user ID than the one searched,
	 *            typically when a system or admin session is used.
	 * @param cn
	 *            the username of the user
	 */
	public static Node getGroupHome(Session session, String cn) {
		try {
			QueryObjectModelFactory qomf = session.getWorkspace().getQueryManager().getQOMFactory();
			Selector sel = qomf.selector(NodeTypes.NODE_GROUP_HOME, "sel");
			DynamicOperand dop = qomf.propertyValue(sel.getSelectorName(), NodeNames.LDAP_CN);
			StaticOperand sop = qomf.literal(session.getValueFactory().createValue(cn));
			Constraint constraint = qomf.comparison(dop, QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO, sop);
			Query query = qomf.createQuery(sel, constraint, null, null);
			return querySingleNode(query);
		} catch (RepositoryException e) {
			throw new RuntimeException("Cannot find home for user " + cn, e);
		}
	}

	/**
	 * Queries one single node.
	 * 
	 * @return one single node or null if none was found
	 * @throws ArgeoJcrException
	 *             if more than one node was found
	 */
	private static Node querySingleNode(Query query) {
		NodeIterator nodeIterator;
		try {
			QueryResult queryResult = query.execute();
			nodeIterator = queryResult.getNodes();
		} catch (RepositoryException e) {
			throw new RuntimeException("Cannot execute query " + query, e);
		}
		Node node;
		if (nodeIterator.hasNext())
			node = nodeIterator.nextNode();
		else
			return null;

		if (nodeIterator.hasNext())
			throw new RuntimeException("Query returned more than one node.");
		return node;
	}

	/** Returns the home node of the session user or null if none was found. */
	public static Node getUserHome(Session session) {
		String userID = session.getUserID();
		return getUserHome(session, userID);
	}

	// public static Node getUserProfile(Session session, String username) {
	// try {
	// QueryObjectModelFactory qomf = session.getWorkspace()
	// .getQueryManager().getQOMFactory();
	// Selector userHomeSel = qomf.selector(ArgeoTypes.ARGEO_USER_PROFILE,
	// "userProfile");
	// DynamicOperand userIdDop = qomf.propertyValue(
	// userHomeSel.getSelectorName(), ArgeoNames.ARGEO_USER_ID);
	// StaticOperand userIdSop = qomf.literal(session.getValueFactory()
	// .createValue(username));
	// Constraint constraint = qomf.comparison(userIdDop,
	// QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO, userIdSop);
	// Query query = qomf.createQuery(userHomeSel, constraint, null, null);
	// return querySingleNode(query);
	// } catch (RepositoryException e) {
	// throw new RuntimeException(
	// "Cannot find profile for user " + username, e);
	// }
	// }
	//
}
