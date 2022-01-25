package org.argeo.cms.acr.xml;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.spi.AbstractContent;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedSession;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class DomContent extends AbstractContent implements ProvidedContent {

	private final ProvidedSession session;
	private final DomContentProvider provider;
	private final Element element;

//	private String text = null;
	private Boolean hasText = null;

	public DomContent(ProvidedSession session, DomContentProvider contentProvider, Element element) {
		this.session = session;
		this.provider = contentProvider;
		this.element = element;
	}

	public DomContent(DomContent context, Element element) {
		this(context.getSession(), context.getProvider(), element);
	}

	@Override
	public QName getName() {
		return toQName(this.element);
	}

	protected QName toQName(Node node) {
		String prefix = node.getPrefix();
		if (prefix == null) {
			String namespaceURI = node.getNamespaceURI();
			if (namespaceURI == null)
				namespaceURI = node.getOwnerDocument().lookupNamespaceURI(null);
			if (namespaceURI == null) {
				return toQName(node, node.getLocalName());
			} else {
				String contextPrefix = session.getPrefix(namespaceURI);
				if (contextPrefix == null)
					throw new IllegalStateException("Namespace " + namespaceURI + " is unbound");
				return toQName(node, namespaceURI, node.getLocalName(), session);
			}
		} else {
			String namespaceURI = node.getNamespaceURI();
			if (namespaceURI == null)
				namespaceURI = node.getOwnerDocument().lookupNamespaceURI(prefix);
			if (namespaceURI == null) {
				namespaceURI = session.getNamespaceURI(prefix);
				if (XMLConstants.NULL_NS_URI.equals(namespaceURI))
					throw new IllegalStateException("Prefix " + prefix + " is unbound");
				// TODO bind the prefix in the document?
			}
			return toQName(node, namespaceURI, node.getLocalName(), session);
		}
	}

	protected QName toQName(Node source, String namespaceURI, String localName, NamespaceContext namespaceContext) {
		return new ContentName(namespaceURI, localName, session);
	}

	protected QName toQName(Node source, String localName) {
		return new ContentName(localName);
	}
	/*
	 * ATTRIBUTES OPERATIONS
	 */

	@Override
	public Iterable<QName> keys() {
		// TODO implement an iterator?
		Set<QName> result = new HashSet<>();
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Attr attr = (Attr) attributes.item(i);
			QName key = toQName(attr);
			result.add(key);
		}
		return result;
	}

	@Override
	public <A> Optional<A> get(QName key, Class<A> clss) {
		String namespaceUriOrNull = XMLConstants.NULL_NS_URI.equals(key.getNamespaceURI()) ? null
				: key.getNamespaceURI();
		if (element.hasAttributeNS(namespaceUriOrNull, key.getLocalPart())) {
			String value = element.getAttributeNS(namespaceUriOrNull, key.getLocalPart());
			if (clss.isAssignableFrom(String.class))
				return Optional.of((A) value);
			else
				return Optional.empty();
		} else
			return null;
	}

	@Override
	public Object put(QName key, Object value) {
		Object previous = get(key);
		String namespaceUriOrNull = XMLConstants.NULL_NS_URI.equals(key.getNamespaceURI()) ? null
				: key.getNamespaceURI();
		element.setAttributeNS(namespaceUriOrNull,
				namespaceUriOrNull == null ? key.getLocalPart() : key.getPrefix() + ":" + key.getLocalPart(),
				value.toString());
		return previous;
	}
	
	

	@Override
	public boolean hasText() {
//		return element instanceof Text;
		if (hasText != null)
			return hasText;
		NodeList nodeList = element.getChildNodes();
		if (nodeList.getLength() > 1) {
			hasText = false;
			return hasText;
		}
		nodes: for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);
			if (node instanceof Text) {
				Text text = (Text) node;
				if (!text.isElementContentWhitespace()) {
					hasText = true;
					break nodes;
				}
			}
		}
		if (hasText == null)
			hasText = false;
		return hasText;
//		if (text != null)
//			return true;
//		text = element.getTextContent();
//		return text != null;
	}

	@Override
	public String getText() {
		if (hasText())
			return element.getTextContent();
		else
			return null;
	}

	/*
	 * CONTENT OPERATIONS
	 */

	@Override
	public Iterator<Content> iterator() {
		NodeList nodeList = element.getChildNodes();
		return new ElementIterator(session, provider, nodeList);
	}

	@Override
	public Content getParent() {
		Node parent = element.getParentNode();
		if (parent == null)
			return null;
		if (!(parent instanceof Element))
			throw new IllegalStateException("Parent is not an element");
		return new DomContent(this, (Element) parent);
	}

	@Override
	public Content add(QName name, QName... classes) {
		// TODO consider classes
		Document document = this.element.getOwnerDocument();
		String namespaceUriOrNull = XMLConstants.NULL_NS_URI.equals(name.getNamespaceURI()) ? null
				: name.getNamespaceURI();
		Element child = document.createElementNS(namespaceUriOrNull,
				namespaceUriOrNull == null ? name.getLocalPart() : name.getPrefix() + ":" + name.getLocalPart());
		element.appendChild(child);
		return new DomContent(this, child);
	}

	@Override
	public void remove() {
		// TODO make it more robust
		element.getParentNode().removeChild(element);

	}

	@Override
	protected void removeAttr(QName key) {
		String namespaceUriOrNull = XMLConstants.NULL_NS_URI.equals(key.getNamespaceURI()) ? null
				: key.getNamespaceURI();
		element.removeAttributeNS(namespaceUriOrNull,
				namespaceUriOrNull == null ? key.getLocalPart() : key.getPrefix() + ":" + key.getLocalPart());

	}

	public ProvidedSession getSession() {
		return session;
	}

	public DomContentProvider getProvider() {
		return provider;
	}

}