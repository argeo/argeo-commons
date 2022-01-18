package org.argeo.cms.jcr.gcr;

import java.util.Calendar;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;

import org.argeo.api.gcr.Content;
import org.argeo.api.gcr.ContentName;
import org.argeo.api.gcr.spi.AbstractContent;
import org.argeo.jcr.Jcr;
import org.argeo.jcr.JcrException;

public class JcrContent extends AbstractContent {
	private JcrContentProvider contentProvider;
	private Node jcrNode;

	protected JcrContent(JcrContentProvider contentSession, Node node) {
		this.contentProvider = contentSession;
		this.jcrNode = node;
	}

	@Override
	public String getName() {
		return Jcr.getName(jcrNode);
	}

	@Override
	public <A> A get(String key, Class<A> clss) {
		if (isDefaultAttrTypeRequested(clss)) {
			return (A) get(jcrNode, key);
		}
		return (A) Jcr.get(jcrNode, key);
	}

	@Override
	public Iterator<Content> iterator() {
		try {
			return new JcrContentIterator(contentProvider, jcrNode.getNodes());
		} catch (RepositoryException e) {
			throw new JcrException("Cannot list children of " + jcrNode, e);
		}
	}

	@Override
	protected Iterable<String> keys() {
		return new Iterable<String>() {

			@Override
			public Iterator<String> iterator() {
				try {
					PropertyIterator propertyIterator = jcrNode.getProperties();
					return new JcrKeyIterator(contentProvider, propertyIterator);
				} catch (RepositoryException e) {
					throw new JcrException("Cannot retrive properties from " + jcrNode, e);
				}
			}
		};
	}

	public Node getJcrNode() {
		return jcrNode;
	}

	/** Cast to a standard Java object. */
	static Object get(Node node, String property) {
		try {
			Value value = node.getProperty(property).getValue();
			switch (value.getType()) {
			case PropertyType.STRING:
				return value.getString();
			case PropertyType.DOUBLE:
				return (Double) value.getDouble();
			case PropertyType.LONG:
				return (Long) value.getLong();
			case PropertyType.BOOLEAN:
				return (Boolean) value.getBoolean();
			case PropertyType.DATE:
				Calendar calendar = value.getDate();
				return calendar.toInstant();
			case PropertyType.BINARY:
				throw new IllegalArgumentException("Binary is not supported as an attribute");
			default:
				return value.getString();
			}
		} catch (RepositoryException e) {
			throw new JcrException("Cannot cast value from " + property + " of node " + node, e);
		}
	}

	static class JcrContentIterator implements Iterator<Content> {
		private final JcrContentProvider contentSession;
		private final NodeIterator nodeIterator;
		// we keep track in order to be able to delete it
		private JcrContent current = null;

		protected JcrContentIterator(JcrContentProvider contentSession, NodeIterator nodeIterator) {
			this.contentSession = contentSession;
			this.nodeIterator = nodeIterator;
		}

		@Override
		public boolean hasNext() {
			return nodeIterator.hasNext();
		}

		@Override
		public Content next() {
			current = new JcrContent(contentSession, nodeIterator.nextNode());
			return current;
		}

		@Override
		public void remove() {
			if (current != null) {
				Jcr.remove(current.getJcrNode());
			}
		}

	}

	@Override
	public Content getParent() {
		return new JcrContent(contentProvider, Jcr.getParent(getJcrNode()));
	}

	@Override
	public Content add(String name, ContentName... classes) {
		if (classes.length > 0) {
			ContentName primaryType = classes[0];
			Node child = Jcr.addNode(getJcrNode(), name, primaryType.toString());
			for (int i = 1; i < classes.length; i++) {
				try {
					child.addMixin(classes[i].toString());
				} catch (RepositoryException e) {
					throw new JcrException("Cannot add child to " + getJcrNode(), e);
				}
			}

		} else {
			Jcr.addNode(getJcrNode(), name, NodeType.NT_UNSTRUCTURED);
		}
		return null;
	}

	@Override
	public void remove() {
		Jcr.remove(getJcrNode());
	}

	@Override
	protected void removeAttr(String key) {
		Property property = Jcr.getProperty(getJcrNode(), key);
		if (property != null) {
			try {
				property.remove();
			} catch (RepositoryException e) {
				throw new JcrException("Cannot remove property " + key + " from " + getJcrNode(), e);
			}
		}

	}

	static class JcrKeyIterator implements Iterator<String> {
		private final JcrContentProvider contentSession;
		private final PropertyIterator propertyIterator;

		protected JcrKeyIterator(JcrContentProvider contentSession, PropertyIterator propertyIterator) {
			this.contentSession = contentSession;
			this.propertyIterator = propertyIterator;
		}

		@Override
		public boolean hasNext() {
			return propertyIterator.hasNext();
		}

		@Override
		public String next() {
			Property property = null;
			try {
				property = propertyIterator.nextProperty();
				// TODO map standard property names
				return property.getName();
			} catch (RepositoryException e) {
				throw new JcrException("Cannot retrieve property " + property, null);
			}
		}

	}
}
