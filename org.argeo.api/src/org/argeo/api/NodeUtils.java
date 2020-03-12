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
package org.argeo.api;

import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.AuthPermission;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/** Utilities related to Argeo model in JCR */
public class NodeUtils {
	/**
	 * Wraps the call to the repository factory based on parameter
	 * {@link NodeConstants#CN} in order to simplify it and protect against future
	 * API changes.
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
	 * {@link NodeConstants#LABELED_URI} in order to simplify it and protect against
	 * future API changes.
	 */
	public static Repository getRepositoryByUri(RepositoryFactory repositoryFactory, String uri) {
		return getRepositoryByUri(repositoryFactory, uri, null);
	}

	/**
	 * Wraps the call to the repository factory based on parameter
	 * {@link NodeConstants#LABELED_URI} in order to simplify it and protect against
	 * future API changes.
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

	/**
	 * Returns the home node of the user or null if none was found.
	 * 
	 * @param session  the session to use in order to perform the search, this can
	 *                 be a session with a different user ID than the one searched,
	 *                 typically when a system or admin session is used.
	 * @param username the username of the user
	 */
	public static Node getUserHome(Session session, String username) {
//		try {
//			QueryObjectModelFactory qomf = session.getWorkspace().getQueryManager().getQOMFactory();
//			Selector sel = qomf.selector(NodeTypes.NODE_USER_HOME, "sel");
//			DynamicOperand dop = qomf.propertyValue(sel.getSelectorName(), NodeNames.LDAP_UID);
//			StaticOperand sop = qomf.literal(session.getValueFactory().createValue(username));
//			Constraint constraint = qomf.comparison(dop, QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO, sop);
//			Query query = qomf.createQuery(sel, constraint, null, null);
//			return querySingleNode(query);
//		} catch (RepositoryException e) {
//			throw new RuntimeException("Cannot find home for user " + username, e);
//		}

		try {
			checkUserWorkspace(session, username);
			String homePath = getHomePath(username);
			if (session.itemExists(homePath))
				return session.getNode(homePath);
			// legacy
			homePath = "/home/" + username;
			if (session.itemExists(homePath))
				return session.getNode(homePath);
			return null;
		} catch (RepositoryException e) {
			throw new RuntimeException("Cannot find home for user " + username, e);
		}
	}

	private static String getHomePath(String username) {
		LdapName dn;
		try {
			dn = new LdapName(username);
		} catch (InvalidNameException e) {
			throw new IllegalArgumentException("Invalid name " + username, e);
		}
		String userId = dn.getRdn(dn.size() - 1).getValue().toString();
		return '/' + userId;
	}

	private static void checkUserWorkspace(Session session, String username) {
		String workspaceName = session.getWorkspace().getName();
		if (!NodeConstants.HOME.equals(workspaceName))
			throw new IllegalArgumentException(workspaceName + " is not the home workspace for user " + username);
	}

	/**
	 * Returns the home node of the user or null if none was found.
	 * 
	 * @param session   the session to use in order to perform the search, this can
	 *                  be a session with a different user ID than the one searched,
	 *                  typically when a system or admin session is used.
	 * @param groupname the name of the group
	 */
	public static Node getGroupHome(Session session, String groupname) {
//		try {
//			QueryObjectModelFactory qomf = session.getWorkspace().getQueryManager().getQOMFactory();
//			Selector sel = qomf.selector(NodeTypes.NODE_GROUP_HOME, "sel");
//			DynamicOperand dop = qomf.propertyValue(sel.getSelectorName(), NodeNames.LDAP_CN);
//			StaticOperand sop = qomf.literal(session.getValueFactory().createValue(cn));
//			Constraint constraint = qomf.comparison(dop, QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO, sop);
//			Query query = qomf.createQuery(sel, constraint, null, null);
//			return querySingleNode(query);
//		} catch (RepositoryException e) {
//			throw new RuntimeException("Cannot find home for group " + cn, e);
//		}

		try {
			checkGroupWorkspace(session, groupname);
			String homePath = getGroupPath(groupname);
			if (session.itemExists(homePath))
				return session.getNode(homePath);
			// legacy
			homePath = "/groups/" + groupname;
			if (session.itemExists(homePath))
				return session.getNode(homePath);
			return null;
		} catch (RepositoryException e) {
			throw new RuntimeException("Cannot find home for group " + groupname, e);
		}

	}

	private static String getGroupPath(String groupname) {
		String cn;
		try {
			LdapName dn = new LdapName(groupname);
			cn = dn.getRdn(dn.size() - 1).getValue().toString();
		} catch (InvalidNameException e) {
			cn = groupname;
		}
		return '/' + cn;
	}

	private static void checkGroupWorkspace(Session session, String groupname) {
		String workspaceName = session.getWorkspace().getName();
		if (!NodeConstants.SRV.equals(workspaceName))
			throw new IllegalArgumentException(workspaceName + " is not the group workspace for group " + groupname);
	}

	/**
	 * Queries one single node.
	 * 
	 * @return one single node or null if none was found
	 * @throws ArgeoJcrException if more than one node was found
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

	/**
	 * Translate the path to this node into a path containing the name of the
	 * repository and the name of the workspace.
	 */
	public static String getDataPath(String cn, Node node) throws RepositoryException {
		assert node != null;
		StringBuilder buf = new StringBuilder(NodeConstants.PATH_DATA);
		return buf.append('/').append(cn).append('/').append(node.getSession().getWorkspace().getName())
				.append(node.getPath()).toString();
	}

	/**
	 * Open a JCR session with full read/write rights on the data, as
	 * {@link NodeConstants#ROLE_USER_ADMIN}, using the
	 * {@link NodeConstants#LOGIN_CONTEXT_DATA_ADMIN} login context. For security
	 * hardened deployement, use {@link AuthPermission} on this login context.
	 */
	public static Session openDataAdminSession(Repository repository, String workspaceName) {
		ClassLoader currentCl = Thread.currentThread().getContextClassLoader();
		LoginContext loginContext;
		try {
			loginContext = new LoginContext(NodeConstants.LOGIN_CONTEXT_DATA_ADMIN);
			loginContext.login();
		} catch (LoginException e1) {
			throw new RuntimeException("Could not login as data admin", e1);
		} finally {
			Thread.currentThread().setContextClassLoader(currentCl);
		}
		return Subject.doAs(loginContext.getSubject(), new PrivilegedAction<Session>() {

			@Override
			public Session run() {
				try {
					return repository.login(workspaceName);
				} catch (NoSuchWorkspaceException e) {
					throw new IllegalArgumentException("No workspace " + workspaceName + " available", e);
				} catch (RepositoryException e) {
					throw new RuntimeException("Cannot open data admin session", e);
				}
			}

		});
	}

	/** Singleton. */
	private NodeUtils() {
	}

}
