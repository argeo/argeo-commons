package org.argeo.jcr;

import java.util.Calendar;
import java.util.StringTokenizer;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;

public class JcrUtils {
	private final static Log log = LogFactory.getLog(JcrUtils.class);

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

	public static String dateAsPath(Calendar cal) {
		StringBuffer buf = new StringBuffer(11);
		buf.append(cal.get(Calendar.YEAR));// 4
		buf.append('/');// 1
		int month = cal.get(Calendar.MONTH) + 1;
		if (month < 10)
			buf.append(0);
		buf.append(month);// 2
		buf.append('/');// 1
		int day = cal.get(Calendar.DAY_OF_MONTH);
		if (day < 10)
			buf.append(0);
		buf.append(day);// 2
		buf.append('/');// 1
		return buf.toString();

	}

	public static String lastPathElement(String path) {
		if (path.charAt(path.length() - 1) == '/')
			throw new ArgeoException("Path " + path + " cannot end with '/'");
		int index = path.lastIndexOf('/');
		if (index < 0)
			throw new ArgeoException("Cannot find last path element for "
					+ path);
		return path.substring(index + 1);
	}

	public static Node mkdirs(Session session, String path) {
		return mkdirs(session, path, null, false);
	}

	public static Node mkdirs(Session session, String path, String type,
			Boolean versioning) {
		try {
			if (path.equals('/'))
				return session.getRootNode();

			StringTokenizer st = new StringTokenizer(path, "/");
			StringBuffer current = new StringBuffer("/");
			Node currentNode = session.getRootNode();
			while (st.hasMoreTokens()) {
				String part = st.nextToken();
				current.append(part).append('/');
				if (!session.itemExists(current.toString())) {
					if (type != null)
						currentNode = currentNode.addNode(part, type);
					else
						currentNode = currentNode.addNode(part);
					if (versioning)
						currentNode.addMixin(ArgeoJcrConstants.MIX_VERSIONABLE);
					if (log.isTraceEnabled())
						log.debug("Added folder " + part + " as " + current);
				} else {
					currentNode = (Node) session.getItem(current.toString());
				}
			}
			session.save();
			return currentNode;
		} catch (RepositoryException e) {
			throw new ArgeoException("Cannot mkdirs " + path, e);
		}
	}

	/** Recursively outputs the contents of the given node. */
	public static void debug(Node node) throws RepositoryException {
		// First output the node path
		log.debug(node.getPath());
		// Skip the virtual (and large!) jcr:system subtree
		if (node.getName().equals(ArgeoJcrConstants.JCR_SYSTEM)) {
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
		while (properties.hasNext()) {
			Property property = properties.nextProperty();
			if (property.getDefinition().isMultiple()) {
				// A multi-valued property, print all values
				Value[] values = property.getValues();
				for (int i = 0; i < values.length; i++) {
					log.debug(property.getPath() + "=" + values[i].getString());
				}
			} else {
				// A single-valued property
				log.debug(property.getPath() + "=" + property.getString());
			}
		}

	}
}
