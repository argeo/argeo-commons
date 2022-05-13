package org.argeo.cms.jcr.acr;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeType;
import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.NamespaceUtils;
import org.argeo.api.acr.spi.AbstractContent;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.jcr.Jcr;
import org.argeo.jcr.JcrException;

/** A JCR {@link Node} accessed as {@link Content}. */
public class JcrContent extends AbstractContent {
	private Node jcrNode;

	private JcrContentProvider provider;
	private ProvidedSession session;

	protected JcrContent(ProvidedSession session, JcrContentProvider provider, Node node) {
		this.session = session;
		this.provider = provider;
		this.jcrNode = node;
	}

	@Override
	public QName getName() {
		String name = Jcr.getName(jcrNode);
		if (name.equals("")) {// root
			name = Jcr.getWorkspaceName(jcrNode);
		}
		return NamespaceUtils.parsePrefixedName(provider, name);
	}

	@Override
	public <A> Optional<A> get(QName key, Class<A> clss) {
		if (isDefaultAttrTypeRequested(clss)) {
			return Optional.of((A) get(jcrNode, key.toString()));
		}
		return Optional.of((A) Jcr.get(jcrNode, key.toString()));
	}

	@Override
	public Iterator<Content> iterator() {
		try {
			return new JcrContentIterator(jcrNode.getNodes());
		} catch (RepositoryException e) {
			throw new JcrException("Cannot list children of " + jcrNode, e);
		}
	}

	@Override
	protected Iterable<QName> keys() {
		try {
			Set<QName> keys = new HashSet<>();
			properties: for (PropertyIterator propertyIterator = jcrNode.getProperties(); propertyIterator.hasNext();) {
				Property property = propertyIterator.nextProperty();
				// TODO convert standard names
				// TODO skip technical properties
				QName name = NamespaceUtils.parsePrefixedName(provider, property.getName());
				keys.add(name);
			}
			return keys;
		} catch (RepositoryException e) {
			throw new JcrException("Cannot list properties of " + jcrNode, e);
		}

//		return new Iterable<QName>() {
//
//			@Override
//			public Iterator<QName> iterator() {
//				try {
//					PropertyIterator propertyIterator = jcrNode.getProperties();
//					return new JcrKeyIterator(provider, propertyIterator);
//				} catch (RepositoryException e) {
//					throw new JcrException("Cannot retrive properties from " + jcrNode, e);
//				}
//			}
//		};
	}

	public Node getJcrNode() {
		return jcrNode;
	}

	/** Cast to a standard Java object. */
	static Object get(Node node, String property) {
		try {
			Property p = node.getProperty(property);
			if (p.isMultiple()) {
				Value[] values = p.getValues();
				List<Object> lst = new ArrayList<>();
				for (Value value : values) {
					lst.add(convertSingleValue(value));
				}
				return lst;
			} else {
				Value value = node.getProperty(property).getValue();
				return convertSingleValue(value);
			}
		} catch (RepositoryException e) {
			throw new JcrException("Cannot cast value from " + property + " of node " + node, e);
		}
	}

	static Object convertSingleValue(Value value) throws RepositoryException {
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
	}

	class JcrContentIterator implements Iterator<Content> {
		private final NodeIterator nodeIterator;
		// we keep track in order to be able to delete it
		private JcrContent current = null;

		protected JcrContentIterator(NodeIterator nodeIterator) {
			this.nodeIterator = nodeIterator;
		}

		@Override
		public boolean hasNext() {
			return nodeIterator.hasNext();
		}

		@Override
		public Content next() {
			current = new JcrContent(session, provider, nodeIterator.nextNode());
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
		return new JcrContent(session, provider, Jcr.getParent(getJcrNode()));
	}

	@Override
	public Content add(QName name, QName... classes) {
		if (classes.length > 0) {
			QName primaryType = classes[0];
			Node child = Jcr.addNode(getJcrNode(), name.toString(), primaryType.toString());
			for (int i = 1; i < classes.length; i++) {
				try {
					child.addMixin(classes[i].toString());
				} catch (RepositoryException e) {
					throw new JcrException("Cannot add child to " + getJcrNode(), e);
				}
			}

		} else {
			Jcr.addNode(getJcrNode(), name.toString(), NodeType.NT_UNSTRUCTURED);
		}
		return null;
	}

	@Override
	public void remove() {
		Jcr.remove(getJcrNode());
	}

	@Override
	protected void removeAttr(QName key) {
		Property property = Jcr.getProperty(getJcrNode(), key.toString());
		if (property != null) {
			try {
				property.remove();
			} catch (RepositoryException e) {
				throw new JcrException("Cannot remove property " + key + " from " + getJcrNode(), e);
			}
		}

	}

	class JcrKeyIterator implements Iterator<QName> {
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
		public QName next() {
			Property property = null;
			try {
				property = propertyIterator.nextProperty();
				// TODO map standard property names
				return NamespaceUtils.parsePrefixedName(provider, property.getName());
			} catch (RepositoryException e) {
				throw new JcrException("Cannot retrieve property " + property, null);
			}
		}

	}
}
