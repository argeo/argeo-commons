package org.argeo.cms.ui.widgets;

import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.cms.swt.CmsSwtUtils;
import org.argeo.jcr.JcrException;
import org.eclipse.swt.widgets.Composite;

/** A composite which can (optionally) manage a JCR Item. */
public class JcrComposite extends Composite {
	private static final long serialVersionUID = -1447009015451153367L;

	private Session session;

	private String nodeId;
	private String property = null;
	private Node cache;

	/** Regular composite constructor. No layout is set. */
	public JcrComposite(Composite parent, int style) {
		super(parent, style);
		session = null;
		nodeId = null;
	}

	public JcrComposite(Composite parent, int style, Item item) {
		this(parent, style, item, false);
	}

	public JcrComposite(Composite parent, int style, Item item, boolean cacheImmediately) {
		super(parent, style);
		if (item != null)
			try {
				this.session = item.getSession();
//				if (!cacheImmediately && (SWT.READ_ONLY == (style & SWT.READ_ONLY))) {
//					// (useless?) optimization: we only save a pointer to the session,
//					// not even a reference to the item
//					this.nodeId = null;
//				} else {
				Node node;
				Property property = null;
				if (item instanceof Node) {
					node = (Node) item;
				} else {// Property
					property = (Property) item;
					if (property.isMultiple())// TODO manage property index
						throw new UnsupportedOperationException("Multiple properties not supported yet.");
					this.property = property.getName();
					node = property.getParent();
				}
				this.nodeId = node.getIdentifier();
				if (cacheImmediately)
					this.cache = node;
//				}
				setLayout(CmsSwtUtils.noSpaceGridLayout());
			} catch (RepositoryException e) {
				throw new IllegalStateException("Cannot create composite from " + item, e);
			}
	}

	public synchronized Node getNode() {
		try {
			if (!itemIsNode())
				throw new IllegalStateException("Item is not a Node");
			return getNodeInternal();
		} catch (RepositoryException e) {
			throw new JcrException("Cannot get node " + nodeId, e);
		}
	}

	private synchronized Node getNodeInternal() throws RepositoryException {
		if (cache != null)
			return cache;
		else if (session != null)
			if (nodeId != null)
				return session.getNodeByIdentifier(nodeId);
			else
				return null;
		else
			return null;
	}

	public synchronized String getPropertyName() {
		try {
			return getProperty().getName();
		} catch (RepositoryException e) {
			throw new JcrException("Cannot get property name", e);
		}
	}

	public synchronized Node getPropertyNode() {
		try {
			return getProperty().getNode();
		} catch (RepositoryException e) {
			throw new JcrException("Cannot get property name", e);
		}
	}

	public synchronized Property getProperty() {
		try {
			if (itemIsNode())
				throw new IllegalStateException("Item is not a Property");
			Node node = getNodeInternal();
			if (!node.hasProperty(property))
				throw new IllegalStateException("Property " + property + " is not set on " + node);
			return node.getProperty(property);
		} catch (RepositoryException e) {
			throw new JcrException("Cannot get property " + property + " from node " + nodeId, e);
		}
	}

	public synchronized boolean itemIsNode() {
		return property == null;
	}

	public synchronized boolean itemExists() {
		if (session == null)
			return false;
		try {
			Node n = session.getNodeByIdentifier(nodeId);
			if (!itemIsNode())
				return n.hasProperty(property);
			else
				return true;
		} catch (ItemNotFoundException e) {
			return false;
		} catch (RepositoryException e) {
			throw new JcrException("Cannot check whether node exists", e);
		}
	}

	/** Set/update the cache or change the node */
	public synchronized void setNode(Node node) {
		if (!itemIsNode())
			throw new IllegalArgumentException("Cannot set a Node on a Property");

		if (node == null) {// clear cache
			this.cache = null;
			return;
		}

		try {
//			if (session != null || session != node.getSession())// check session
//				throw new IllegalArgumentException("Uncompatible session");
//			if (session == null)
			session = node.getSession();
			if (nodeId == null || !nodeId.equals(node.getIdentifier())) {
				nodeId = node.getIdentifier();
				cache = node;
				itemUpdated();
			} else {
				cache = node;// set/update cache
			}
		} catch (RepositoryException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Set/update the cache or change the property */
	public synchronized void setProperty(Property prop) {
		if (itemIsNode())
			throw new IllegalArgumentException("Cannot set a Property on a Node");

		if (prop == null) {// clear cache
			this.cache = null;
			return;
		}

		try {
			if (session == null || session != prop.getSession())// check session
				throw new IllegalArgumentException("Uncompatible session");

			Node node = prop.getNode();
			if (nodeId == null || !nodeId.equals(node.getIdentifier()) || !property.equals(prop.getName())) {
				nodeId = node.getIdentifier();
				property = prop.getName();
				cache = node;
				itemUpdated();
			} else {
				cache = node;// set/update cache
			}
		} catch (RepositoryException e) {
			throw new IllegalStateException(e);
		}
	}

	public synchronized String getNodeId() {
		return nodeId;
	}

	/** Change the node, does nothing if same. */
	public synchronized void setNodeId(String nodeId) throws RepositoryException {
		if (this.nodeId != null && this.nodeId.equals(nodeId))
			return;
		this.nodeId = nodeId;
		if (cache != null)
			cache = session.getNodeByIdentifier(this.nodeId);
		itemUpdated();
	}

	protected synchronized void itemUpdated() {
		layout();
	}

//	public Session getSession() {
//		return session;
//	}
}
