/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
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

import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.Selector;
import javax.jcr.query.qom.StaticOperand;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;

/** Utility methods to simplify common JCR operations. */
public class JcrUtils implements ArgeoJcrConstants {
	private final static Log log = LogFactory.getLog(JcrUtils.class);

	/** Prevents instantiation */
	private JcrUtils() {
	}

	/**
	 * Queries one single node.
	 * 
	 * @return one single node or null if none was found
	 * @throws ArgeoException
	 *             if more than one node was found
	 */
	public static Node querySingleNode(Query query) {
		NodeIterator nodeIterator;
		try {
			QueryResult queryResult = query.execute();
			nodeIterator = queryResult.getNodes();
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot execute query " + query, e);
		}
		Node node;
		if (nodeIterator.hasNext())
			node = nodeIterator.nextNode();
		else
			return null;

		if (nodeIterator.hasNext())
			throw new ArgeoException("Query returned more than one node.");
		return node;
	}

	/** Removes forbidden characters from a path, replacing them with '_' */
	public static String removeForbiddenCharacters(String str) {
		return str.replace('[', '_').replace(']', '_').replace('/', '_')
				.replace('*', '_');

	}

	/** Retrieves the parent path of the provided path */
	public static String parentPath(String path) {
		if (path.equals("/"))
			throw new ArgeoException("Root path '/' has no parent path");
		if (path.charAt(0) != '/')
			throw new ArgeoException("Path " + path + " must start with a '/'");
		String pathT = path;
		if (pathT.charAt(pathT.length() - 1) == '/')
			pathT = pathT.substring(0, pathT.length() - 2);

		int index = pathT.lastIndexOf('/');
		return pathT.substring(0, index);
	}

	/** The provided data as a path ('/' at the end, not the beginning) */
	public static String dateAsPath(Calendar cal) {
		return dateAsPath(cal, false);
	}

	/**
	 * Creates a deep path based on a URL:
	 * http://subdomain.example.com/to/content?args =>
	 * com/example/subdomain/to/content
	 */
	public static String urlAsPath(String url) {
		try {
			URL u = new URL(url);
			StringBuffer path = new StringBuffer(url.length());
			// invert host
			path.append(hostAsPath(u.getHost()));
			// we don't put port since it may not always be there and may change
			path.append(u.getPath());
			return path.toString();
		} catch (MalformedURLException e) {
			throw new ArgeoException("Cannot generate URL path for " + url, e);
		}
	}

	/**
	 * Creates a path from a FQDN, inverting the order of the component:
	 * www.argeo.org => org.argeo.www
	 */
	public static String hostAsPath(String host) {
		StringBuffer path = new StringBuffer(host.length());
		String[] hostTokens = host.split("\\.");
		for (int i = hostTokens.length - 1; i >= 0; i--) {
			path.append(hostTokens[i]);
			if (i != 0)
				path.append('/');
		}
		return path.toString();
	}

	/**
	 * The provided data as a path ('/' at the end, not the beginning)
	 * 
	 * @param cal
	 *            the date
	 * @param addHour
	 *            whether to add hour as well
	 */
	public static String dateAsPath(Calendar cal, Boolean addHour) {
		StringBuffer buf = new StringBuffer(14);
		buf.append('Y').append(cal.get(Calendar.YEAR));// 5
		buf.append('/');// 1
		int month = cal.get(Calendar.MONTH) + 1;
		buf.append('M');
		if (month < 10)
			buf.append(0);
		buf.append(month);// 3
		buf.append('/');// 1
		int day = cal.get(Calendar.DAY_OF_MONTH);
		if (day < 10)
			buf.append(0);
		buf.append('D').append(day);// 3
		buf.append('/');// 1
		if (addHour) {
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			if (hour < 10)
				buf.append(0);
			buf.append('H').append(hour);// 3
			buf.append('/');// 1
		}
		return buf.toString();

	}

	/** Converts in one call a string into a gregorian calendar. */
	public static Calendar parseCalendar(DateFormat dateFormat, String value) {
		try {
			Date date = dateFormat.parse(value);
			Calendar calendar = new GregorianCalendar();
			calendar.setTime(date);
			return calendar;
		} catch (ParseException e) {
			throw new ArgeoException("Cannot parse " + value
					+ " with date format " + dateFormat, e);
		}

	}

	/** The last element of a path. */
	public static String lastPathElement(String path) {
		if (path.charAt(path.length() - 1) == '/')
			throw new ArgeoException("Path " + path + " cannot end with '/'");
		int index = path.lastIndexOf('/');
		if (index < 0)
			throw new ArgeoException("Cannot find last path element for "
					+ path);
		return path.substring(index + 1);
	}

	/** Creates the nodes making path, if they don't exist. */
	public static Node mkdirs(Session session, String path) {
		return mkdirs(session, path, null, null, false);
	}

	/**
	 * @deprecated use {@link #mkdirs(Session, String, String, String, Boolean)}
	 *             instead.
	 */
	@Deprecated
	public static Node mkdirs(Session session, String path, String type,
			Boolean versioning) {
		return mkdirs(session, path, type, type, false);
	}

	/**
	 * @param type
	 *            the type of the leaf node
	 */
	public static Node mkdirs(Session session, String path, String type) {
		return mkdirs(session, path, type, null, false);
	}

	/**
	 * Creates the nodes making path, if they don't exist. This is up to the
	 * caller to save the session.
	 */
	public static Node mkdirs(Session session, String path, String type,
			String intermediaryNodeType, Boolean versioning) {
		try {
			if (path.equals('/'))
				return session.getRootNode();

			if (session.itemExists(path)) {
				Node node = session.getNode(path);
				// check type
				if (type != null
						&& !type.equals(node.getPrimaryNodeType().getName()))
					throw new ArgeoException("Node " + node
							+ " exists but is of type "
							+ node.getPrimaryNodeType().getName()
							+ " not of type " + type);
				// TODO: check versioning
				return node;
			}

			StringTokenizer st = new StringTokenizer(path, "/");
			StringBuffer current = new StringBuffer("/");
			Node currentNode = session.getRootNode();
			while (st.hasMoreTokens()) {
				String part = st.nextToken();
				current.append(part).append('/');
				if (!session.itemExists(current.toString())) {
					if (!st.hasMoreTokens() && type != null)
						currentNode = currentNode.addNode(part, type);
					else if (st.hasMoreTokens() && intermediaryNodeType != null)
						currentNode = currentNode.addNode(part,
								intermediaryNodeType);
					else
						currentNode = currentNode.addNode(part);
					if (versioning)
						currentNode.addMixin(NodeType.MIX_VERSIONABLE);
					if (log.isTraceEnabled())
						log.debug("Added folder " + part + " as " + current);
				} else {
					currentNode = (Node) session.getItem(current.toString());
				}
			}
			// session.save();
			return currentNode;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot mkdirs " + path, e);
		}
	}

	/**
	 * Safe and repository implementation independent registration of a
	 * namespace.
	 */
	public static void registerNamespaceSafely(Session session, String prefix,
			String uri) {
		try {
			registerNamespaceSafely(session.getWorkspace()
					.getNamespaceRegistry(), prefix, uri);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot find namespace registry", e);
		}
	}

	/**
	 * Safe and repository implementation independent registration of a
	 * namespace.
	 */
	public static void registerNamespaceSafely(NamespaceRegistry nr,
			String prefix, String uri) {
		try {
			String[] prefixes = nr.getPrefixes();
			for (String pref : prefixes)
				if (pref.equals(prefix)) {
					String registeredUri = nr.getURI(pref);
					if (!registeredUri.equals(uri))
						throw new ArgeoException("Prefix " + pref
								+ " already registered for URI "
								+ registeredUri
								+ " which is different from provided URI "
								+ uri);
					else
						return;// skip
				}
			nr.registerNamespace(prefix, uri);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot register namespace " + uri
					+ " under prefix " + prefix, e);
		}
	}

	/** Recursively outputs the contents of the given node. */
	public static void debug(Node node) {
		try {
			// First output the node path
			log.debug(node.getPath());
			// Skip the virtual (and large!) jcr:system subtree
			if (node.getName().equals("jcr:system")) {
				return;
			}

			// Then the children nodes (recursive)
			NodeIterator it = node.getNodes();
			while (it.hasNext()) {
				Node childNode = it.nextNode();
				debug(childNode);
			}

			// Then output the properties
			PropertyIterator properties = node.getProperties();
			// log.debug("Property are : ");

			while (properties.hasNext()) {
				Property property = properties.nextProperty();
				if (property.getDefinition().isMultiple()) {
					// A multi-valued property, print all values
					Value[] values = property.getValues();
					for (int i = 0; i < values.length; i++) {
						log.debug(property.getPath() + "="
								+ values[i].getString());
					}
				} else {
					// A single-valued property
					log.debug(property.getPath() + "=" + property.getString());
				}
			}
		} catch (Exception e) {
			log.error("Could not debug " + node, e);
		}

	}

	/**
	 * Copies recursively the content of a node to another one. Mixin are NOT
	 * copied.
	 */
	public static void copy(Node fromNode, Node toNode) {
		try {
			PropertyIterator pit = fromNode.getProperties();
			properties: while (pit.hasNext()) {
				Property fromProperty = pit.nextProperty();
				String propertyName = fromProperty.getName();
				if (toNode.hasProperty(propertyName)
						&& toNode.getProperty(propertyName).getDefinition()
								.isProtected())
					continue properties;

				toNode.setProperty(fromProperty.getName(),
						fromProperty.getValue());
			}

			NodeIterator nit = fromNode.getNodes();
			while (nit.hasNext()) {
				Node fromChild = nit.nextNode();
				Integer index = fromChild.getIndex();
				String nodeRelPath = fromChild.getName() + "[" + index + "]";
				Node toChild;
				if (toNode.hasNode(nodeRelPath))
					toChild = toNode.getNode(nodeRelPath);
				else
					toChild = toNode.addNode(fromChild.getName(), fromChild
							.getPrimaryNodeType().getName());
				copy(fromChild, toChild);
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot copy " + fromNode + " to "
					+ toNode, e);
		}
	}

	/**
	 * Check whether all first-level properties (except jcr:* properties) are
	 * equal. Skip jcr:* properties
	 */
	public static Boolean allPropertiesEquals(Node reference, Node observed,
			Boolean onlyCommonProperties) {
		try {
			PropertyIterator pit = reference.getProperties();
			props: while (pit.hasNext()) {
				Property propReference = pit.nextProperty();
				String propName = propReference.getName();
				if (propName.startsWith("jcr:"))
					continue props;

				if (!observed.hasProperty(propName))
					if (onlyCommonProperties)
						continue props;
					else
						return false;
				// TODO: deal with multiple property values?
				if (!observed.getProperty(propName).getValue()
						.equals(propReference.getValue()))
					return false;
			}
			return true;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot check all properties equals of "
					+ reference + " and " + observed, e);
		}
	}

	public static Map<String, PropertyDiff> diffProperties(Node reference,
			Node observed) {
		Map<String, PropertyDiff> diffs = new TreeMap<String, PropertyDiff>();
		diffPropertiesLevel(diffs, null, reference, observed);
		return diffs;
	}

	/**
	 * Compare the properties of two nodes. Recursivity to child nodes is not
	 * yet supported. Skip jcr:* properties.
	 */
	static void diffPropertiesLevel(Map<String, PropertyDiff> diffs,
			String baseRelPath, Node reference, Node observed) {
		try {
			// check removed and modified
			PropertyIterator pit = reference.getProperties();
			props: while (pit.hasNext()) {
				Property p = pit.nextProperty();
				String name = p.getName();
				if (name.startsWith("jcr:"))
					continue props;

				if (!observed.hasProperty(name)) {
					String relPath = propertyRelPath(baseRelPath, name);
					PropertyDiff pDiff = new PropertyDiff(PropertyDiff.REMOVED,
							relPath, p.getValue(), null);
					diffs.put(relPath, pDiff);
				} else {
					if (p.isMultiple())
						continue props;
					Value referenceValue = p.getValue();
					Value newValue = observed.getProperty(name).getValue();
					if (!referenceValue.equals(newValue)) {
						String relPath = propertyRelPath(baseRelPath, name);
						PropertyDiff pDiff = new PropertyDiff(
								PropertyDiff.MODIFIED, relPath, referenceValue,
								newValue);
						diffs.put(relPath, pDiff);
					}
				}
			}
			// check added
			pit = observed.getProperties();
			props: while (pit.hasNext()) {
				Property p = pit.nextProperty();
				String name = p.getName();
				if (name.startsWith("jcr:"))
					continue props;
				if (!reference.hasProperty(name)) {
					String relPath = propertyRelPath(baseRelPath, name);
					PropertyDiff pDiff = new PropertyDiff(PropertyDiff.ADDED,
							relPath, null, p.getValue());
					diffs.put(relPath, pDiff);
				}
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot diff " + reference + " and "
					+ observed, e);
		}
	}

	/**
	 * Compare only a restricted list of properties of two nodes. No
	 * recursivity.
	 * 
	 */
	public static Map<String, PropertyDiff> diffProperties(Node reference,
			Node observed, List<String> properties) {
		Map<String, PropertyDiff> diffs = new TreeMap<String, PropertyDiff>();
		try {
			Iterator<String> pit = properties.iterator();

			props: while (pit.hasNext()) {
				String name = pit.next();
				if (!reference.hasProperty(name)) {
					if (!observed.hasProperty(name))
						continue props;
					Value val = observed.getProperty(name).getValue();
					try {
						// empty String but not null
						if ("".equals(val.getString()))
							continue props;
					} catch (Exception e) {
						// not parseable as String, silent
					}
					PropertyDiff pDiff = new PropertyDiff(PropertyDiff.ADDED,
							name, null, val);
					diffs.put(name, pDiff);
				} else if (!observed.hasProperty(name)) {
					PropertyDiff pDiff = new PropertyDiff(PropertyDiff.REMOVED,
							name, reference.getProperty(name).getValue(), null);
					diffs.put(name, pDiff);
				} else {
					Value referenceValue = reference.getProperty(name)
							.getValue();
					Value newValue = observed.getProperty(name).getValue();
					if (!referenceValue.equals(newValue)) {
						PropertyDiff pDiff = new PropertyDiff(
								PropertyDiff.MODIFIED, name, referenceValue,
								newValue);
						diffs.put(name, pDiff);
					}
				}
			}
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot diff " + reference + " and "
					+ observed, e);
		}
		return diffs;
	}

	/** Builds a property relPath to be used in the diff. */
	private static String propertyRelPath(String baseRelPath,
			String propertyName) {
		if (baseRelPath == null)
			return propertyName;
		else
			return baseRelPath + '/' + propertyName;
	}

	/**
	 * Normalize a name so taht it can be stores in contexts not supporting
	 * names with ':' (typically databases). Replaces ':' by '_'.
	 */
	public static String normalize(String name) {
		return name.replace(':', '_');
	}

	/** Cleanly disposes a {@link Binary} even if it is null. */
	public static void closeQuietly(Binary binary) {
		if (binary == null)
			return;
		binary.dispose();
	}

	/**
	 * Creates depth from a string (typically a username) by adding levels based
	 * on its first characters: "aBcD",2 => a/aB
	 */
	public static String firstCharsToPath(String str, Integer nbrOfChars) {
		if (str.length() < nbrOfChars)
			throw new ArgeoException("String " + str
					+ " length must be greater or equal than " + nbrOfChars);
		StringBuffer path = new StringBuffer("");
		StringBuffer curr = new StringBuffer("");
		for (int i = 0; i < nbrOfChars; i++) {
			curr.append(str.charAt(i));
			path.append(curr);
			if (i < nbrOfChars - 1)
				path.append('/');
		}
		return path.toString();
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

	/**
	 * Discards the current changes in a session by calling
	 * {@link Session#refresh(boolean)} with <code>false</code>, only logging
	 * potential errors when doing so. To be used typically in a catch block.
	 */
	public static void discardQuietly(Session session) {
		try {
			if (session != null)
				session.refresh(false);
		} catch (RepositoryException e) {
			log.warn("Cannot quietly discard session " + session + ": "
					+ e.getMessage());
		}
	}

	/** Logs out the session, not throwing any exception, even if it is null. */
	public static void logoutQuietly(Session session) {
		if (session != null)
			session.logout();
	}

	/** Returns the home node of the session user or null if none was found. */
	public static Node getUserHome(Session session) {
		String userID = session.getUserID();
		return getUserHome(session, userID);
	}

	/**
	 * Returns the home node of the session user or null if none was found.
	 * 
	 * @param session
	 *            the session to use in order to perform the search, this can be
	 *            a session with a different user ID than the one searched,
	 *            typically when a system or admin session is used.
	 * @param userID
	 *            the id of the user
	 */
	public static Node getUserHome(Session session, String userID) {
		try {
			QueryObjectModelFactory qomf = session.getWorkspace()
					.getQueryManager().getQOMFactory();

			// query the user home for this user id
			Selector userHomeSel = qomf.selector(ArgeoTypes.ARGEO_USER_HOME,
					"userHome");
			DynamicOperand userIdDop = qomf.propertyValue("userHome",
					ArgeoNames.ARGEO_USER_ID);
			StaticOperand userIdSop = qomf.literal(session.getValueFactory()
					.createValue(userID));
			Constraint constraint = qomf.comparison(userIdDop,
					QueryObjectModelFactory.JCR_OPERATOR_EQUAL_TO, userIdSop);
			Query query = qomf.createQuery(userHomeSel, constraint, null, null);
			Node userHome = JcrUtils.querySingleNode(query);
			return userHome;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot find home for user " + userID, e);
		}
	}
}
