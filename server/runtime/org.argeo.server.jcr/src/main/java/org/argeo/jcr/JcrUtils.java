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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.RepositoryFactory;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.VersionManager;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;

/** Utility methods to simplify common JCR operations. */
public class JcrUtils implements ArgeoJcrConstants {

	private final static Log log = LogFactory.getLog(JcrUtils.class);

	/**
	 * Not complete yet. See
	 * http://www.day.com/specs/jcr/2.0/3_Repository_Model.html#3.2.2%20Local
	 * %20Names
	 */
	public final static char[] INVALID_NAME_CHARACTERS = { '/', ':', '[', ']',
			'|', '*', /*
					 * invalid XML chars :
					 */
			'<', '>', '&' };

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

	/** Set the {@link NodeType#NT_ADDRESS} properties based on this URL. */
	public static void urlToAddressProperties(Node node, String url) {
		try {
			URL u = new URL(url);
			node.setProperty(Property.JCR_PROTOCOL, u.getProtocol());
			node.setProperty(Property.JCR_HOST, u.getHost());
			node.setProperty(Property.JCR_PORT, Integer.toString(u.getPort()));
			node.setProperty(Property.JCR_PATH, normalizePath(u.getPath()));
		} catch (Exception e) {
			throw new ArgeoException("Cannot set URL " + url
					+ " as nt:address properties", e);
		}
	}

	/** Build URL based on the {@link NodeType#NT_ADDRESS} properties. */
	public static String urlFromAddressProperties(Node node) {
		try {
			URL u = new URL(
					node.getProperty(Property.JCR_PROTOCOL).getString(), node
							.getProperty(Property.JCR_HOST).getString(),
					(int) node.getProperty(Property.JCR_PORT).getLong(), node
							.getProperty(Property.JCR_PATH).getString());
			return u.toString();
		} catch (Exception e) {
			throw new ArgeoException(
					"Cannot get URL from nt:address properties of " + node, e);
		}
	}

	/** Make sure that: starts with '/', do not end with '/', do not have '//' */
	public static String normalizePath(String path) {
		List<String> tokens = tokenize(path);
		StringBuffer buf = new StringBuffer(path.length());
		for (String token : tokens) {
			buf.append('/');
			buf.append(token);
		}
		return buf.toString();
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
		buf.append('Y');
		buf.append(cal.get(Calendar.YEAR));
		buf.append('/');

		int month = cal.get(Calendar.MONTH) + 1;
		buf.append('M');
		if (month < 10)
			buf.append(0);
		buf.append(month);
		buf.append('/');

		int day = cal.get(Calendar.DAY_OF_MONTH);
		buf.append('D');
		if (day < 10)
			buf.append(0);
		buf.append(day);
		buf.append('/');

		if (addHour) {
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			buf.append('H');
			if (hour < 10)
				buf.append(0);
			buf.append(hour);
			buf.append('/');
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

	/**
	 * Routine that get the child with this name, adding id it does not already
	 * exist
	 */
	public static Node getOrAdd(Node parent, String childName,
			String childPrimaryNodeType) throws RepositoryException {
		return parent.hasNode(childName) ? parent.getNode(childName) : parent
				.addNode(childName, childPrimaryNodeType);
	}

	/**
	 * Routine that get the child with this name, adding id it does not already
	 * exist
	 */
	public static Node getOrAdd(Node parent, String childName)
			throws RepositoryException {
		return parent.hasNode(childName) ? parent.getNode(childName) : parent
				.addNode(childName);
	}

	/** Creates the nodes making path, if they don't exist. */
	public static Node mkdirs(Session session, String path) {
		return mkdirs(session, path, null, null, false);
	}

	/**
	 * use {@link #mkdirs(Session, String, String, String, Boolean)} instead.
	 * 
	 * @deprecated
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
	 * Synchronized and save is performed, to avoid race conditions in
	 * initializers leading to duplicate nodes.
	 */
	public synchronized static Node mkdirsSafe(Session session, String path,
			String type) {
		try {
			if (session.hasPendingChanges())
				throw new ArgeoException(
						"Session has pending changes, save them first.");
			Node node = mkdirs(session, path, type);
			session.save();
			return node;
		} catch (RepositoryException e) {
			discardQuietly(session);
			throw new ArgeoException("Cannot safely make directories", e);
		}
	}

	public synchronized static Node mkdirsSafe(Session session, String path) {
		return mkdirsSafe(session, path, null);
	}

	/**
	 * Creates the nodes making path, if they don't exist. This is up to the
	 * caller to save the session. Use with caution since it can create
	 * duplicate nodes if used concurrently.
	 */
	public static Node mkdirs(Session session, String path, String type,
			String intermediaryNodeType, Boolean versioning) {
		try {
			if (path.equals('/'))
				return session.getRootNode();

			if (session.itemExists(path)) {
				Node node = session.getNode(path);
				// check type
				if (type != null && !node.isNodeType(type))
					throw new ArgeoException("Node " + node
							+ " exists but is of type "
							+ node.getPrimaryNodeType().getName()
							+ " not of type " + type);
				// TODO: check versioning
				return node;
			}

			StringBuffer current = new StringBuffer("/");
			Node currentNode = session.getRootNode();
			Iterator<String> it = tokenize(path).iterator();
			while (it.hasNext()) {
				String part = it.next();
				current.append(part).append('/');
				if (!session.itemExists(current.toString())) {
					if (!it.hasNext() && type != null)
						currentNode = currentNode.addNode(part, type);
					else if (it.hasNext() && intermediaryNodeType != null)
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
			return currentNode;
		} catch (RepositoryException e) {
			discardQuietly(session);
			throw new ArgeoException("Cannot mkdirs " + path, e);
		} finally {
		}
	}

	/** Convert a path to the list of its tokens */
	public static List<String> tokenize(String path) {
		List<String> tokens = new ArrayList<String>();
		boolean optimized = false;
		if (!optimized) {
			String[] rawTokens = path.split("/");
			for (String token : rawTokens) {
				if (!token.equals(""))
					tokens.add(token);
			}
		} else {
			StringBuffer curr = new StringBuffer();
			char[] arr = path.toCharArray();
			chars: for (int i = 0; i < arr.length; i++) {
				char c = arr[i];
				if (c == '/') {
					if (i == 0 || (i == arr.length - 1))
						continue chars;
					if (curr.length() > 0) {
						tokens.add(curr.toString());
						curr = new StringBuffer();
					}
				} else
					curr.append(c);
			}
			if (curr.length() > 0) {
				tokens.add(curr.toString());
				curr = new StringBuffer();
			}
		}
		return Collections.unmodifiableList(tokens);
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
		debug(node, log);
	}

	/** Recursively outputs the contents of the given node. */
	public static void debug(Node node, Log log) {
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
				debug(childNode, log);
			}

			// Then output the properties
			PropertyIterator properties = node.getProperties();
			// log.debug("Property are : ");

			properties: while (properties.hasNext()) {
				Property property = properties.nextProperty();
				if (property.getType() == PropertyType.BINARY)
					continue properties;// skip
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
	 * Copies recursively the content of a node to another one. Do NOT copy the
	 * property values of {@link NodeType#MIX_CREATED} and
	 * {@link NodeType#MIX_LAST_MODIFIED}, but update the
	 * {@link Property#JCR_LAST_MODIFIED} and
	 * {@link Property#JCR_LAST_MODIFIED_BY} properties if the target node has
	 * the {@link NodeType#MIX_LAST_MODIFIED} mixin.
	 */
	public static void copy(Node fromNode, Node toNode) {
		try {
			// process properties
			PropertyIterator pit = fromNode.getProperties();
			properties: while (pit.hasNext()) {
				Property fromProperty = pit.nextProperty();
				String propertyName = fromProperty.getName();
				if (toNode.hasProperty(propertyName)
						&& toNode.getProperty(propertyName).getDefinition()
								.isProtected())
					continue properties;

				if (fromProperty.getDefinition().isProtected())
					continue properties;

				if (propertyName.equals("jcr:created")
						|| propertyName.equals("jcr:createdBy")
						|| propertyName.equals("jcr:lastModified")
						|| propertyName.equals("jcr:lastModifiedBy"))
					continue properties;

				if (fromProperty.isMultiple()) {
					toNode.setProperty(propertyName, fromProperty.getValues());
				} else {
					toNode.setProperty(propertyName, fromProperty.getValue());
				}
			}

			// update jcr:lastModified and jcr:lastModifiedBy in toNode in case
			// they existed, before adding the mixins
			updateLastModified(toNode);

			// add mixins
			for (NodeType mixinType : fromNode.getMixinNodeTypes()) {
				toNode.addMixin(mixinType.getName());
			}

			// process children nodes
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
					if (p.isMultiple()) {
						// FIXME implement multiple
					} else {
						Value referenceValue = p.getValue();
						Value newValue = observed.getProperty(name).getValue();
						if (!referenceValue.equals(newValue)) {
							String relPath = propertyRelPath(baseRelPath, name);
							PropertyDiff pDiff = new PropertyDiff(
									PropertyDiff.MODIFIED, relPath,
									referenceValue, newValue);
							diffs.put(relPath, pDiff);
						}
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
					if (p.isMultiple()) {
						// FIXME implement multiple
					} else {
						String relPath = propertyRelPath(baseRelPath, name);
						PropertyDiff pDiff = new PropertyDiff(
								PropertyDiff.ADDED, relPath, null, p.getValue());
						diffs.put(relPath, pDiff);
					}
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
	 * Normalizes a name so that it can be stored in contexts not supporting
	 * names with ':' (typically databases). Replaces ':' by '_'.
	 */
	public static String normalize(String name) {
		return name.replace(':', '_');
	}

	/**
	 * Replaces characters which are invalid in a JCR name by '_'. Currently not
	 * exhaustive.
	 * 
	 * @see JcrUtils#INVALID_NAME_CHARACTERS
	 */
	public static String replaceInvalidChars(String name) {
		return replaceInvalidChars(name, '_');
	}

	/**
	 * Replaces characters which are invalid in a JCR name. Currently not
	 * exhaustive.
	 * 
	 * @see JcrUtils#INVALID_NAME_CHARACTERS
	 */
	public static String replaceInvalidChars(String name, char replacement) {
		boolean modified = false;
		char[] arr = name.toCharArray();
		for (int i = 0; i < arr.length; i++) {
			char c = arr[i];
			invalid: for (char invalid : INVALID_NAME_CHARACTERS) {
				if (c == invalid) {
					arr[i] = replacement;
					modified = true;
					break invalid;
				}
			}
		}
		if (modified)
			return new String(arr);
		else
			// do not create new object if unnecessary
			return name;
	}

	/**
	 * Removes forbidden characters from a path, replacing them with '_'
	 * 
	 * @deprecated use {@link #replaceInvalidChars(String)} instead
	 */
	public static String removeForbiddenCharacters(String str) {
		return str.replace('[', '_').replace(']', '_').replace('/', '_')
				.replace('*', '_');

	}

	/** Cleanly disposes a {@link Binary} even if it is null. */
	public static void closeQuietly(Binary binary) {
		if (binary == null)
			return;
		binary.dispose();
	}

	/** Retrieve a {@link Binary} as a byte array */
	public static byte[] getBinaryAsBytes(Property property) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		InputStream in = null;
		Binary binary = null;
		try {
			binary = property.getBinary();
			in = binary.getStream();
			IOUtils.copy(in, out);
			return out.toByteArray();
		} catch (Exception e) {
			throw new ArgeoException("Cannot read binary " + property
					+ " as bytes", e);
		} finally {
			IOUtils.closeQuietly(out);
			IOUtils.closeQuietly(in);
			closeQuietly(binary);
		}
	}

	/** Writes a {@link Binary} from a byte array */
	public static void setBinaryAsBytes(Node node, String property, byte[] bytes) {
		InputStream in = null;
		Binary binary = null;
		try {
			in = new ByteArrayInputStream(bytes);
			binary = node.getSession().getValueFactory().createBinary(in);
			node.setProperty(property, binary);
		} catch (Exception e) {
			throw new ArgeoException("Cannot read binary " + property
					+ " as bytes", e);
		} finally {
			IOUtils.closeQuietly(in);
			closeQuietly(binary);
		}
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
	 * Discards the current changes in the session attached to this node. To be
	 * used typically in a catch block.
	 * 
	 * @see #discardQuietly(Session)
	 */
	public static void discardUnderlyingSessionQuietly(Node node) {
		try {
			discardQuietly(node.getSession());
		} catch (RepositoryException e) {
			log.warn("Cannot quietly discard session of node " + node + ": "
					+ e.getMessage());
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
		try {
			if (session != null)
				if (session.isLive())
					session.logout();
		} catch (Exception e) {
			// silent
		}
	}

	/**
	 * Convenient method to add a listener. uuids passed as null, deep=true,
	 * local=true, only one node type
	 */
	public static void addListener(Session session, EventListener listener,
			int eventTypes, String basePath, String nodeType) {
		try {
			session.getWorkspace()
					.getObservationManager()
					.addEventListener(listener, eventTypes, basePath, true,
							null, new String[] { nodeType }, true);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot add JCR listener " + listener
					+ " to session " + session, e);
		}
	}

	/** Removes a listener without throwing exception */
	public static void removeListenerQuietly(Session session,
			EventListener listener) {
		if (session == null || !session.isLive())
			return;
		try {
			session.getWorkspace().getObservationManager()
					.removeEventListener(listener);
		} catch (RepositoryException e) {
			// silent
		}
	}

	/** Returns the home node of the session user or null if none was found. */
	public static Node getUserHome(Session session) {
		String userID = session.getUserID();
		return getUserHome(session, userID);
	}

	/** User home path is NOT configurable */
	public static String getUserHomePath(String username) {
		String homeBasePath = DEFAULT_HOME_BASE_PATH;
		return homeBasePath + '/' + firstCharsToPath(username, 2) + '/'
				+ username;
	}

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
			String homePath = getUserHomePath(username);
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

	/**
	 * Creates an Argeo user home, does nothing if it already exists. Session is
	 * NOT saved.
	 */
	public static Node createUserHomeIfNeeded(Session session, String username) {
		try {
			String homePath = getUserHomePath(username);
			if (session.itemExists(homePath))
				return session.getNode(homePath);
			else {
				Node userHome = JcrUtils.mkdirs(session, homePath);
				userHome.addMixin(ArgeoTypes.ARGEO_USER_HOME);
				userHome.setProperty(ArgeoNames.ARGEO_USER_ID, username);
				return userHome;
			}
		} catch (RepositoryException e) {
			discardQuietly(session);
			throw new ArgeoException("Cannot create home for " + username
					+ " in workspace " + session.getWorkspace().getName(), e);
		}
	}

	/**
	 * Creates a user profile in the home of this user. Creates the home if
	 * needed, but throw an exception if a profile already exists. The session
	 * is not saved and the node is in a checkedOut state (that is, it requires
	 * a subsequent checkin after saving the session).
	 */
	public static Node createUserProfile(Session session, String username) {
		try {
			Node userHome = createUserHomeIfNeeded(session, username);
			if (userHome.hasNode(ArgeoNames.ARGEO_PROFILE))
				throw new ArgeoException(
						"There is already a user profile under " + userHome);
			Node userProfile = userHome.addNode(ArgeoNames.ARGEO_PROFILE);
			userProfile.addMixin(ArgeoTypes.ARGEO_USER_PROFILE);
			userProfile.setProperty(ArgeoNames.ARGEO_USER_ID, username);
			userProfile.setProperty(ArgeoNames.ARGEO_ENABLED, true);
			userProfile.setProperty(ArgeoNames.ARGEO_ACCOUNT_NON_EXPIRED, true);
			userProfile.setProperty(ArgeoNames.ARGEO_ACCOUNT_NON_LOCKED, true);
			userProfile.setProperty(ArgeoNames.ARGEO_CREDENTIALS_NON_EXPIRED,
					true);
			return userProfile;
		} catch (RepositoryException e) {
			discardQuietly(session);
			throw new ArgeoException("Cannot create user profile for "
					+ username + " in workspace "
					+ session.getWorkspace().getName(), e);
		}
	}

	/**
	 * Create user profile if needed, the session IS saved.
	 * 
	 * @return the user profile
	 */
	public static Node createUserProfileIfNeeded(Session securitySession,
			String username) {
		try {
			Node userHome = JcrUtils.createUserHomeIfNeeded(securitySession,
					username);
			Node userProfile = userHome.hasNode(ArgeoNames.ARGEO_PROFILE) ? userHome
					.getNode(ArgeoNames.ARGEO_PROFILE) : JcrUtils
					.createUserProfile(securitySession, username);
			if (securitySession.hasPendingChanges())
				securitySession.save();
			VersionManager versionManager = securitySession.getWorkspace()
					.getVersionManager();
			if (versionManager.isCheckedOut(userProfile.getPath()))
				versionManager.checkin(userProfile.getPath());
			return userProfile;
		} catch (RepositoryException e) {
			discardQuietly(securitySession);
			throw new ArgeoException("Cannot create user profile for "
					+ username + " in workspace "
					+ securitySession.getWorkspace().getName(), e);
		}
	}

	/** Creates an Argeo user home. */
	// public static Node createUserHome(Session session, String homeBasePath,
	// String username) {
	// try {
	// if (session == null)
	// throw new ArgeoException("Session is null");
	// if (session.hasPendingChanges())
	// throw new ArgeoException(
	// "Session has pending changes, save them first");
	//
	// String homePath = getUserHomePath(username);
	//
	// if (session.itemExists(homePath)) {
	// try {
	// throw new ArgeoException(
	// "Trying to create a user home that already exists");
	// } catch (Exception e) {
	// // we use this workaround to be sure to get the stack trace
	// // to identify the sink of the bug.
	// log.warn("trying to create an already existing userHome at path:"
	// + homePath + ". Stack trace : ");
	// e.printStackTrace();
	// }
	// }
	//
	// Node userHome = JcrUtils.mkdirs(session, homePath);
	// Node userProfile;
	// if (userHome.hasNode(ArgeoNames.ARGEO_PROFILE)) {
	// log.warn("userProfile node already exists for userHome path: "
	// + homePath + ". We do not add a new one");
	// } else {
	// userProfile = userHome.addNode(ArgeoNames.ARGEO_PROFILE);
	// userProfile.addMixin(ArgeoTypes.ARGEO_USER_PROFILE);
	// // session.getWorkspace().getVersionManager()
	// // .checkout(userProfile.getPath());
	// userProfile.setProperty(ArgeoNames.ARGEO_USER_ID, username);
	// session.save();
	// session.getWorkspace().getVersionManager()
	// .checkin(userProfile.getPath());
	// // we need to save the profile before adding the user home type
	// }
	// userHome.addMixin(ArgeoTypes.ARGEO_USER_HOME);
	// // see
	// //
	// http://jackrabbit.510166.n4.nabble.com/Jackrabbit-2-0-beta-6-Problem-adding-a-Mixin-type-with-mandatory-properties-after-setting-propertiesn-td1290332.html
	// userHome.setProperty(ArgeoNames.ARGEO_USER_ID, username);
	// session.save();
	// return userHome;
	// } catch (RepositoryException e) {
	// discardQuietly(session);
	// throw new ArgeoException("Cannot create home node for user "
	// + username, e);
	// }
	// }

	/**
	 * Returns user home has path, embedding exceptions. Contrary to
	 * {@link #getUserHome(Session)}, it never returns null but throws and
	 * exception if not found.
	 * 
	 * @deprecated use getUserHome() instead, throwing an exception if it
	 *             returns null
	 */
	@Deprecated
	public static String getUserHomePath(Session session) {
		String userID = session.getUserID();
		try {
			String homePath = getUserHomePath(userID);
			if (session.itemExists(homePath))
				return homePath;
			else
				throw new ArgeoException("No home registered for " + userID);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot find user home path", e);
		}
	}

	/**
	 * @return null if not found *
	 */
	public static Node getUserProfile(Session session, String username) {
		try {
			Node userHome = getUserHome(session, username);
			if (userHome == null)
				return null;
			if (userHome.hasNode(ArgeoNames.ARGEO_PROFILE))
				return userHome.getNode(ArgeoNames.ARGEO_PROFILE);
			else
				return null;
		} catch (RepositoryException e) {
			throw new ArgeoException(
					"Cannot find profile for user " + username, e);
		}
	}

	/**
	 * Get the profile of the user attached to this session.
	 */
	public static Node getUserProfile(Session session) {
		String userID = session.getUserID();
		return getUserProfile(session, userID);
	}

	/**
	 * Quietly unregisters an {@link EventListener} from the udnerlying
	 * workspace of this node.
	 */
	public static void unregisterQuietly(Node node, EventListener eventListener) {
		try {
			unregisterQuietly(node.getSession().getWorkspace(), eventListener);
		} catch (RepositoryException e) {
			// silent
			if (log.isTraceEnabled())
				log.trace("Could not unregister event listener "
						+ eventListener);
		}
	}

	/** Quietly unregisters an {@link EventListener} from this workspace */
	public static void unregisterQuietly(Workspace workspace,
			EventListener eventListener) {
		if (eventListener == null)
			return;
		try {
			workspace.getObservationManager()
					.removeEventListener(eventListener);
		} catch (RepositoryException e) {
			// silent
			if (log.isTraceEnabled())
				log.trace("Could not unregister event listener "
						+ eventListener);
		}
	}

	/**
	 * If this node is has the {@link NodeType#MIX_LAST_MODIFIED} mixin, it
	 * updates the {@link Property#JCR_LAST_MODIFIED} property with the current
	 * time and the {@link Property#JCR_LAST_MODIFIED_BY} property with the
	 * underlying session user id. In Jackrabbit 2.x, <a
	 * href="https://issues.apache.org/jira/browse/JCR-2233">these properties
	 * are not automatically updated</a>, hence the need for manual update. The
	 * session is not saved.
	 */
	public static void updateLastModified(Node node) {
		try {
			if (!node.isNodeType(NodeType.MIX_LAST_MODIFIED))
				node.addMixin(NodeType.MIX_LAST_MODIFIED);
			node.setProperty(Property.JCR_LAST_MODIFIED,
					new GregorianCalendar());
			node.setProperty(Property.JCR_LAST_MODIFIED_BY, node.getSession()
					.getUserID());
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot update last modified on " + node,
					e);
		}
	}

	/** Update lastModified recursively until this parent. */
	public static void updateLastModifiedAndParents(Node node, String untilPath) {
		try {
			if (!node.getPath().startsWith(untilPath))
				throw new ArgeoException(node + " is not under " + untilPath);
			updateLastModified(node);
			if (!node.getPath().equals(untilPath))
				updateLastModifiedAndParents(node.getParent(), untilPath);
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot update lastModified from " + node
					+ " until " + untilPath, e);
		}
	}

	/**
	 * Returns a String representing the short version (see <a
	 * href="http://jackrabbit.apache.org/node-type-notation.html"> Node type
	 * Notation </a> attributes grammar) of the main business attributes of this
	 * property definition
	 * 
	 * @param prop
	 */
	public static String getPropertyDefinitionAsString(Property prop) {
		StringBuffer sbuf = new StringBuffer();
		try {
			if (prop.getDefinition().isAutoCreated())
				sbuf.append("a");
			if (prop.getDefinition().isMandatory())
				sbuf.append("m");
			if (prop.getDefinition().isProtected())
				sbuf.append("p");
			if (prop.getDefinition().isMultiple())
				sbuf.append("*");
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"unexpected error while getting property definition as String",
					re);
		}
		return sbuf.toString();
	}

	/**
	 * Estimate the sub tree size from current node. Computation is based on the
	 * Jcr {@link Property.getLength()} method. Note : it is not the exact size
	 * used on the disk by the current part of the JCR Tree.
	 */

	public static long getNodeApproxSize(Node node) {
		long curNodeSize = 0;
		try {
			PropertyIterator pi = node.getProperties();
			while (pi.hasNext()) {
				Property prop = pi.nextProperty();
				if (prop.isMultiple()) {
					int nb = prop.getLengths().length;
					for (int i = 0; i < nb; i++) {
						curNodeSize += (prop.getLengths()[i] > 0 ? prop
								.getLengths()[i] : 0);
					}
				} else
					curNodeSize += (prop.getLength() > 0 ? prop.getLength() : 0);
			}

			NodeIterator ni = node.getNodes();
			while (ni.hasNext())
				curNodeSize += getNodeApproxSize(ni.nextNode());
			return curNodeSize;
		} catch (RepositoryException re) {
			throw new ArgeoException(
					"Unexpected error while recursively determining node size.",
					re);
		}
	}
}
