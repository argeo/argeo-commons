package org.argeo.cms.widgets;

import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.argeo.cms.CmsException;
import org.argeo.cms.CmsUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

/** A composite which can (optionally) manage a JCR Item. */
public class JcrComposite extends Composite {
	private static final long serialVersionUID = -1447009015451153367L;

	private final Session session;

	private String nodeId;
	private String property = null;
	private Node cache;

	/** Regular composite constructor. No layout is set. */
	public JcrComposite(Composite parent, int style) {
		super(parent, style);
		session = null;
		nodeId = null;
	}

	public JcrComposite(Composite parent, int style, Item item)
			throws RepositoryException {
		this(parent, style, item, false);
	}

	public JcrComposite(Composite parent, int style, Item item,
			boolean cacheImmediately) throws RepositoryException {
		super(parent, style);
		this.session = item.getSession();
		if (!cacheImmediately && (SWT.READ_ONLY == (style & SWT.READ_ONLY))) {
			// (useless?) optimization: we only save a pointer to the session,
			// not even a reference to the item
			this.nodeId = null;
		} else {
			Node node;
			Property property = null;
			if (item instanceof Node) {
				node = (Node) item;
			} else {// Property
				property = (Property) item;
				if (property.isMultiple())// TODO manage property index
					throw new CmsException(
							"Multiple properties not supported yet.");
				this.property = property.getName();
				node = property.getParent();
			}
			this.nodeId = node.getIdentifier();
			if (cacheImmediately)
				this.cache = node;
		}
		setLayout(CmsUtils.noSpaceGridLayout());
	}

	public synchronized Node getNode() {
		try {
			if (!itemIsNode())
				throw new CmsException("Item is not a Node");
			return getNodeInternal();
		} catch (RepositoryException e) {
			throw new CmsException("Cannot get node " + nodeId, e);
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

	public synchronized Property getProperty() {
		try {
			if (itemIsNode())
				throw new CmsException("Item is not a Property");
			Node node = getNodeInternal();
			if (!node.hasProperty(property))
				throw new CmsException("Property " + property
						+ " is not set on " + node);
			return node.getProperty(property);
		} catch (RepositoryException e) {
			throw new CmsException("Cannot get property " + property
					+ " from node " + nodeId, e);
		}
	}

	public synchronized Boolean itemIsNode() {
		return property == null;
	}

	/** Set/update the cache or change the node */
	public synchronized void setNode(Node node) throws RepositoryException {
		if (!itemIsNode())
			throw new CmsException("Cannot set a Node on a Property");

		if (node == null) {// clear cache
			this.cache = null;
			return;
		}

		if (session == null || session != node.getSession())// check session
			throw new CmsException("Uncompatible session");

		if (nodeId == null || !nodeId.equals(node.getIdentifier())) {
			nodeId = node.getIdentifier();
			cache = node;
			itemUpdated();
		} else {
			cache = node;// set/update cache
		}
	}

	/** Set/update the cache or change the property */
	public synchronized void setProperty(Property prop)
			throws RepositoryException {
		if (itemIsNode())
			throw new CmsException("Cannot set a Property on a Node");

		if (prop == null) {// clear cache
			this.cache = null;
			return;
		}

		if (session == null || session != prop.getSession())// check session
			throw new CmsException("Uncompatible session");

		Node node = prop.getNode();
		if (nodeId == null || !nodeId.equals(node.getIdentifier())
				|| !property.equals(prop.getName())) {
			nodeId = node.getIdentifier();
			property = prop.getName();
			cache = node;
			itemUpdated();
		} else {
			cache = node;// set/update cache
		}
	}

	public synchronized String getNodeId() {
		return nodeId;
	}

	/** Change the node, does nothing if same. */
	public synchronized void setNodeId(String nodeId)
			throws RepositoryException {
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

	public Session getSession() {
		return session;
	}
}
