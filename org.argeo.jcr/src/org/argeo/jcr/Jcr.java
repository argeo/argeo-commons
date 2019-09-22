package org.argeo.jcr;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;

/**
 * Utility class whose purpose is to make using JCR less verbose by
 * systematically using unchecked exceptions and returning <code>null</code>
 * when something is not found. This is especially useful when writing user
 * interfaces (such as with SWT) where listeners and callbacks expect unchecked
 * exceptions. Loosely inspired by Java's <code>Files</code> singleton.
 */
public class Jcr {

	/**
	 * @see Node#isNodeType(String)
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static boolean isNodeType(Node node, String nodeTypeName) {
		try {
			return node.isNodeType(nodeTypeName);
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get whether " + node + " is of type " + nodeTypeName, e);
		}
	}

	/**
	 * @see Node#hasNodes()
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static boolean hasNodes(Node node) {
		try {
			return node.hasNodes();
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get whether " + node + " has children.", e);
		}
	}

	/**
	 * @see Node#getParent()
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static Node getParent(Node node) {
		try {
			return isRoot(node) ? null : node.getParent();
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get parent of " + node, e);
		}
	}

	/**
	 * Whether this node is the root node.
	 * 
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static boolean isRoot(Node node) {
		try {
			return node.getDepth() == 0;
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get depth of " + node, e);
		}
	}

	/**
	 * @see Node#getPath()
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static String getPath(Node node) {
		try {
			return node.getPath();
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get path of " + node, e);
		}
	}

	/**
	 * @see Node#getIdentifier()
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static String getIdentifier(Node node) {
		try {
			return node.getIdentifier();
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get identifier of " + node, e);
		}
	}

	/**
	 * @see Node#getName()
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static String getName(Node node) {
		try {
			return node.getName();
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get name of " + node, e);
		}
	}

	/** Accesses a {@link NodeIterator} as an {@link Iterable}. */
	@SuppressWarnings("unchecked")
	public static Iterable<Node> iterate(NodeIterator nodeIterator) {
		return new Iterable<Node>() {

			@Override
			public Iterator<Node> iterator() {
				return nodeIterator;
			}
		};
	}

	/**
	 * @return the children as an {@link Iterable} for use in for-each llops.
	 * @see Node#getNodes()
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static Iterable<Node> nodes(Node node) {
		try {
			return iterate(node.getNodes());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get children of " + node, e);
		}
	}

	/**
	 * @return the children as a (possibly empty) {@link List}.
	 * @see Node#getNodes()
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static List<Node> getNodes(Node node) {
		List<Node> nodes = new ArrayList<>();
		try {
			if (node.hasNodes()) {
				NodeIterator nit = node.getNodes();
				while (nit.hasNext())
					nodes.add(nit.nextNode());
				return nodes;
			} else
				return nodes;
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get children of " + node, e);
		}
	}

	/**
	 * @return the child or <code>null</node> if not found
	 * @see Node#getNode(String)
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static Node getNode(Node node, String child) {
		try {
			if (node.hasNode(child))
				return node.getNode(child);
			else
				return null;
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get child of " + node, e);
		}
	}

	/**
	 * @return the node at this path or <code>null</node> if not found
	 * @see Session#getNode(String)
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static Node getNode(Session session, String path) {
		try {
			if (session.nodeExists(path))
				return session.getNode(path);
			else
				return null;
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get node " + path, e);
		}
	}

	/**
	 * @return the node with htis id or <code>null</node> if not found
	 * @see Session#getNodeByIdentifier(String)
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static Node getNodeById(Session session, String id) {
		try {
			return session.getNodeByIdentifier(id);
		} catch (ItemNotFoundException e) {
			return null;
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get node with id " + id, e);
		}
	}

	/**
	 * Get property as {@link String}.
	 * 
	 * @return the value of
	 *         {@link Node#getProperty(String)}.{@link Property#getString()} or
	 *         <code>null</code> if the property does not exist.
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static String get(Node node, String property) {
		return get(node, property, null);
	}

	/**
	 * Get property as a {@link String}.
	 * 
	 * @return the value of
	 *         {@link Node#getProperty(String)}.{@link Property#getString()} or
	 *         <code>defaultValue</code> if the property does not exist.
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static String get(Node node, String property, String defaultValue) {
		try {
			if (node.hasProperty(property))
				return node.getProperty(property).getString();
			else
				return defaultValue;
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot retrieve property " + property + " from " + node);
		}
	}

	/**
	 * Get property as a {@link Value}.
	 * 
	 * @return {@link Node#getProperty(String)} or <code>null</code> if the property
	 *         does not exist.
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static Value getValue(Node node, String property) {
		try {
			if (node.hasProperty(property))
				return node.getProperty(property).getValue();
			else
				return null;
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot retrieve property " + property + " from " + node);
		}
	}

	/**
	 * Get property doing a best effort to cast it as the target object.
	 * 
	 * @return the value of {@link Node#getProperty(String)} or
	 *         <code>defaultValue</code> if the property does not exist.
	 * @throws IllegalArgumentException if the value could not be cast
	 * @throws IllegalStateException    in case of unexpected
	 *                                  {@link RepositoryException}
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getAs(Node node, String property, T defaultValue) {
		try {
			if (node.hasProperty(property)) {
				Property p = node.getProperty(property);
				try {
					switch (p.getType()) {
					case PropertyType.STRING:
						return (T) node.getProperty(property).getString();
					case PropertyType.DOUBLE:
						return (T) (Double) node.getProperty(property).getDouble();
					case PropertyType.LONG:
						return (T) (Long) node.getProperty(property).getLong();
					case PropertyType.BOOLEAN:
						return (T) (Boolean) node.getProperty(property).getBoolean();
					case PropertyType.DATE:
						return (T) node.getProperty(property).getDate();
					default:
						return (T) node.getProperty(property).getString();
					}
				} catch (ClassCastException e) {
					throw new IllegalArgumentException(
							"Cannot cast property of type " + PropertyType.nameFromValue(p.getType()), e);
				}
			} else {
				return defaultValue;
			}
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot retrieve property " + property + " from " + node);
		}
	}

	/** Login to a JCR repository. */
	public static Session login(Repository repository, String workspace) {
		try {
			return repository.login(workspace);
		} catch (RepositoryException e) {
			throw new IllegalArgumentException("Cannot login to repository", e);
		}
	}

	/** Safely and silently logs out a session. */
	public static void logout(Session session) {
		try {
			if (session != null)
				if (session.isLive())
					session.logout();
		} catch (Exception e) {
			// silent
		}
	}

	/** Singleton. */
	private Jcr() {

	}
}
