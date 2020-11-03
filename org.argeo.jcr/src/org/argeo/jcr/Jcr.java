package org.argeo.jcr;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyType;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.security.Privilege;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionIterator;
import javax.jcr.version.VersionManager;

/**
 * Utility class whose purpose is to make using JCR less verbose by
 * systematically using unchecked exceptions and returning <code>null</code>
 * when something is not found. This is especially useful when writing user
 * interfaces (such as with SWT) where listeners and callbacks expect unchecked
 * exceptions. Loosely inspired by Java's <code>Files</code> singleton.
 */
public class Jcr {
	/**
	 * The name of a node which will be serialized as XML text, as per section 7.3.1
	 * of the JCR 2.0 specifications.
	 */
	public final static String JCR_XMLTEXT = "jcr:xmltext";
	/**
	 * The name of a property which will be serialized as XML text, as per section
	 * 7.3.1 of the JCR 2.0 specifications.
	 */
	public final static String JCR_XMLCHARACTERS = "jcr:xmlcharacters";
	/**
	 * <code>jcr:name</code>, when used in another context than
	 * {@link Property#JCR_NAME}, typically to name a node rather than a property.
	 */
	public final static String JCR_NAME = "jcr:name";
	/**
	 * <code>jcr:path</code>, when used in another context than
	 * {@link Property#JCR_PATH}, typically to name a node rather than a property.
	 */
	public final static String JCR_PATH = "jcr:path";
	/**
	 * <code>jcr:primaryType</code> with prefix instead of namespace (as in
	 * {@link Property#JCR_PRIMARY_TYPE}.
	 */
	public final static String JCR_PRIMARY_TYPE = "jcr:primaryType";
	/**
	 * <code>jcr:mixinTypes</code> with prefix instead of namespace (as in
	 * {@link Property#JCR_MIXIN_TYPES}.
	 */
	public final static String JCR_MIXIN_TYPES = "jcr:mixinTypes";
	/**
	 * <code>jcr:uuid</code> with prefix instead of namespace (as in
	 * {@link Property#JCR_UUID}.
	 */
	public final static String JCR_UUID = "jcr:uuid";
	/**
	 * <code>jcr:created</code> with prefix instead of namespace (as in
	 * {@link Property#JCR_CREATED}.
	 */
	public final static String JCR_CREATED = "jcr:created";
	/**
	 * <code>jcr:createdBy</code> with prefix instead of namespace (as in
	 * {@link Property#JCR_CREATED_BY}.
	 */
	public final static String JCR_CREATED_BY = "jcr:createdBy";
	/**
	 * <code>jcr:lastModified</code> with prefix instead of namespace (as in
	 * {@link Property#JCR_LAST_MODIFIED}.
	 */
	public final static String JCR_LAST_MODIFIED = "jcr:lastModified";
	/**
	 * <code>jcr:lastModifiedBy</code> with prefix instead of namespace (as in
	 * {@link Property#JCR_LAST_MODIFIED_BY}.
	 */
	public final static String JCR_LAST_MODIFIED_BY = "jcr:lastModifiedBy";

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
	 * @see Node#getSession()
	 * @see Session#getWorkspace()
	 * @see Workspace#getName()
	 */
	public static String getWorkspaceName(Node node) {
		return session(node).getWorkspace().getName();
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

	/**
	 * If node has mixin {@link NodeType#MIX_TITLE}, return
	 * {@link Property#JCR_TITLE}, otherwise return {@link #getName(Node)}.
	 */
	public static String getTitle(Node node) {
		if (Jcr.isNodeType(node, NodeType.MIX_TITLE))
			return get(node, Property.JCR_TITLE);
		else
			return Jcr.getName(node);
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
	 * Set a property to the given value, or remove it if the value is
	 * <code>null</code>.
	 * 
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static void set(Node node, String property, Object value) {
		try {
			if (!node.hasProperty(property))
				throw new IllegalArgumentException("No property " + property + " in " + node);
			Property prop = node.getProperty(property);
			if (value == null) {
				prop.remove();
				return;
			}

			if (value instanceof String)
				prop.setValue((String) value);
			else if (value instanceof Long)
				prop.setValue((Long) value);
			else if (value instanceof Double)
				prop.setValue((Double) value);
			else if (value instanceof Calendar)
				prop.setValue((Calendar) value);
			else if (value instanceof BigDecimal)
				prop.setValue((BigDecimal) value);
			else if (value instanceof Boolean)
				prop.setValue((Boolean) value);
			else if (value instanceof byte[])
				JcrUtils.setBinaryAsBytes(prop, (byte[]) value);
			else if (value instanceof Instant) {
				Instant instant = (Instant) value;
				GregorianCalendar calendar = new GregorianCalendar();
				calendar.setTime(Date.from(instant));
				prop.setValue(calendar);
			} else // try with toString()
				prop.setValue(value.toString());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot set property " + property + " of " + node + " to " + value, e);
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
	 * Get property as a {@link String}. If the property is multiple it returns the
	 * first value.
	 * 
	 * @return the value of
	 *         {@link Node#getProperty(String)}.{@link Property#getString()} or
	 *         <code>defaultValue</code> if the property does not exist.
	 * @throws IllegalStateException caused by {@link RepositoryException}
	 */
	public static String get(Node node, String property, String defaultValue) {
		try {
			if (node.hasProperty(property)) {
				Property p = node.getProperty(property);
				if (!p.isMultiple())
					return p.getString();
				else {
					Value[] values = p.getValues();
					if (values.length == 0)
						return defaultValue;
					else
						return values[0].getString();
				}
			} else
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
			// TODO deal with multiple
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

	/**
	 * Retrieves the {@link Session} related to this node.
	 * 
	 * @deprecated Use {@link #getSession(Node)} instead.
	 */
	@Deprecated
	public static Session session(Node node) {
		return getSession(node);
	}

	/** Retrieves the {@link Session} related to this node. */
	public static Session getSession(Node node) {
		try {
			return node.getSession();
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot retrieve session related to " + node, e);
		}
	}

	/** Retrieves the root node related to this session. */
	public static Node getRootNode(Session session) {
		try {
			return session.getRootNode();
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get root node for " + session, e);
		}
	}

	/**
	 * Saves the {@link Session} related to this node. Note that all other unrelated
	 * modifications in this session will also be saved.
	 */
	public static void save(Node node) {
		try {
			Session session = node.getSession();
//			if (node.isNodeType(NodeType.MIX_LAST_MODIFIED)) {
//				set(node, Property.JCR_LAST_MODIFIED, Instant.now());
//				set(node, Property.JCR_LAST_MODIFIED_BY, session.getUserID());
//			}
			if (session.hasPendingChanges())
				session.save();
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot save session related to " + node + " in workspace "
					+ session(node).getWorkspace().getName(), e);
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

	/** Safely and silently logs out the underlying session. */
	public static void logout(Node node) {
		Jcr.logout(session(node));
	}

	/*
	 * SECURITY
	 */
	/**
	 * Add a single privilege to a node.
	 * 
	 * @see Privilege
	 */
	public static void addPrivilege(Node node, String principal, String privilege) {
		try {
			Session session = node.getSession();
			JcrUtils.addPrivilege(session, node.getPath(), principal, privilege);
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot add privilege " + privilege + " to " + node, e);
		}
	}

	/*
	 * VERSIONING
	 */
	/** Get checked out status. */
	public static boolean isCheckedOut(Node node) {
		try {
			return node.isCheckedOut();
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot retrieve checked out status of " + node, e);
		}
	}

	/** @see VersionManager#checkpoint(String) */
	public static void checkpoint(Node node) {
		try {
			versionManager(node).checkpoint(node.getPath());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot check in " + node, e);
		}
	}

	/** @see VersionManager#checkin(String) */
	public static void checkin(Node node) {
		try {
			versionManager(node).checkin(node.getPath());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot check in " + node, e);
		}
	}

	/** @see VersionManager#checkout(String) */
	public static void checkout(Node node) {
		try {
			versionManager(node).checkout(node.getPath());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot check out " + node, e);
		}
	}

	/** Get the {@link VersionManager} related to this node. */
	public static VersionManager versionManager(Node node) {
		try {
			return node.getSession().getWorkspace().getVersionManager();
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get version manager from " + node, e);
		}
	}

	/** Get the {@link VersionHistory} related to this node. */
	public static VersionHistory getVersionHistory(Node node) {
		try {
			return versionManager(node).getVersionHistory(node.getPath());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get version history from " + node, e);
		}
	}

	/**
	 * The linear versions of this version history in reverse order and without the
	 * root version.
	 */
	public static List<Version> getLinearVersions(VersionHistory versionHistory) {
		try {
			List<Version> lst = new ArrayList<>();
			VersionIterator vit = versionHistory.getAllLinearVersions();
			while (vit.hasNext())
				lst.add(vit.nextVersion());
			lst.remove(0);
			Collections.reverse(lst);
			return lst;
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get linear versions from " + versionHistory, e);
		}
	}

	/** The frozen node related to this {@link Version}. */
	public static Node getFrozenNode(Version version) {
		try {
			return version.getFrozenNode();
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get frozen node from " + version, e);
		}
	}

	/** Get the base {@link Version} related to this node. */
	public static Version getBaseVersion(Node node) {
		try {
			return versionManager(node).getBaseVersion(node.getPath());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get base version from " + node, e);
		}
	}

	/*
	 * FILES
	 */
	/**
	 * Returns the size of this file.
	 * 
	 * @see NodeType#NT_FILE
	 */
	public static long getFileSize(Node fileNode) {
		try {
			if (!fileNode.isNodeType(NodeType.NT_FILE))
				throw new IllegalArgumentException(fileNode + " must be a file.");
			return getBinarySize(fileNode.getNode(Node.JCR_CONTENT).getProperty(Property.JCR_DATA).getBinary());
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get file size of " + fileNode, e);
		}
	}

	/** Returns the size of this {@link Binary}. */
	public static long getBinarySize(Binary binaryArg) {
		try {
			try (Bin binary = new Bin(binaryArg)) {
				return binary.getSize();
			}
		} catch (RepositoryException e) {
			throw new IllegalStateException("Cannot get file size of binary " + binaryArg, e);
		}
	}

	/** Singleton. */
	private Jcr() {

	}
}
