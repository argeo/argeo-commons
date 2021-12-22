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

import org.argeo.api.gcr.Content;
import org.argeo.api.gcr.spi.AbstractContent;
import org.argeo.jcr.Jcr;
import org.argeo.jcr.JcrException;

public class JcrContent extends AbstractContent {
	private JcrContentSession contentSession;
	private Node jcrNode;

	protected JcrContent(JcrContentSession contentSession, Node node) {
		this.contentSession = contentSession;
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
			return new JcrContentIterator(contentSession, jcrNode.getNodes());
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
					return new JcrKeyIterator(contentSession, propertyIterator);
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
		private final JcrContentSession contentSession;
		private final NodeIterator nodeIterator;
		// we keep track in order to be able to delete it
		private JcrContent current = null;

		protected JcrContentIterator(JcrContentSession contentSession, NodeIterator nodeIterator) {
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
				// current.getJcrNode().remove();
			}
			throw new UnsupportedOperationException();
		}

	}

	static class JcrKeyIterator implements Iterator<String> {
		private final JcrContentSession contentSession;
		private final PropertyIterator propertyIterator;

		protected JcrKeyIterator(JcrContentSession contentSession, PropertyIterator propertyIterator) {
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
