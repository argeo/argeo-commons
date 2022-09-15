package org.argeo.cms.acr.xml;

import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentName;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.spi.ProvidedContent;
import org.argeo.api.acr.spi.ProvidedSession;
import org.argeo.cms.acr.AbstractContent;
import org.argeo.cms.acr.ContentUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/** Content persisted as a DOM element. */
public class DomContent extends AbstractContent implements ProvidedContent {

	private final DomContentProvider provider;
	private final Element element;

//	private String text = null;
	private Boolean hasText = null;

	public DomContent(ProvidedSession session, DomContentProvider contentProvider, Element element) {
		super(session);
		this.provider = contentProvider;
		this.element = element;
	}

	public DomContent(DomContent context, Element element) {
		this(context.getSession(), context.getProvider(), element);
	}

	@Override
	public QName getName() {
		if (isLocalRoot()) {// root
			String mountPath = provider.getMountPath();
			if (mountPath != null) {
				if (ContentUtils.ROOT_SLASH.equals(mountPath)) {
					return CrName.root.qName();
				}
				Content mountPoint = getSession().getMountPoint(mountPath);
				QName mountPointName = mountPoint.getName();
				return mountPointName;
			}
		}
		return toQName(this.element);
	}

	protected boolean isLocalRoot() {
		return element.getParentNode() == null || element.getParentNode() instanceof Document;
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
				String contextPrefix = provider.getPrefix(namespaceURI);
				if (contextPrefix == null)
					throw new IllegalStateException("Namespace " + namespaceURI + " is unbound");
				return toQName(node, namespaceURI, node.getLocalName(), provider);
			}
		} else {
			String namespaceURI = node.getNamespaceURI();
			if (namespaceURI == null)
				namespaceURI = node.getOwnerDocument().lookupNamespaceURI(prefix);
			if (namespaceURI == null) {
				namespaceURI = provider.getNamespaceURI(prefix);
				if (XMLConstants.NULL_NS_URI.equals(namespaceURI))
					throw new IllegalStateException("Prefix " + prefix + " is unbound");
				// TODO bind the prefix in the document?
			}
			return toQName(node, namespaceURI, node.getLocalName(), provider);
		}
	}

	protected QName toQName(Node source, String namespaceURI, String localName, NamespaceContext namespaceContext) {
		return new ContentName(namespaceURI, localName, namespaceContext);
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
			if (key.getNamespaceURI().equals(XMLConstants.XMLNS_ATTRIBUTE_NS_URI))
				continue;// skip prefix mapping
			result.add(key);
		}
		return result;
	}

	@SuppressWarnings("unchecked")
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
			return Optional.empty();
	}

	@Override
	public Object put(QName key, Object value) {
		Object previous = get(key);
		String namespaceUriOrNull = XMLConstants.NULL_NS_URI.equals(key.getNamespaceURI()) ? null
				: key.getNamespaceURI();
		String prefixToUse = registerPrefixIfNeeded(key);
		element.setAttributeNS(namespaceUriOrNull,
				namespaceUriOrNull == null ? key.getLocalPart() : prefixToUse + ":" + key.getLocalPart(),
				value.toString());
		return previous;
	}

	protected String registerPrefixIfNeeded(QName name) {
		String namespaceUriOrNull = XMLConstants.NULL_NS_URI.equals(name.getNamespaceURI()) ? null
				: name.getNamespaceURI();
		String prefixToUse;
		if (namespaceUriOrNull != null) {
			String registeredPrefix = provider.getPrefix(namespaceUriOrNull);
			if (registeredPrefix != null) {
				prefixToUse = registeredPrefix;
			} else {
				provider.registerPrefix(name.getPrefix(), namespaceUriOrNull);
				prefixToUse = name.getPrefix();
			}
		} else {
			prefixToUse = null;
		}
		return prefixToUse;
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
		return new ElementIterator(this, getSession(), provider, nodeList);
	}

	@Override
	public Content getParent() {
		Node parentNode = element.getParentNode();
		if (isLocalRoot()) {
			String mountPath = provider.getMountPath();
			if (mountPath == null)
				return null;
			if (ContentUtils.ROOT_SLASH.equals(mountPath)) {
				return null;
			}
			String[] parent = ContentUtils.getParentPath(mountPath);
			if (ContentUtils.EMPTY.equals(parent[0]))
				return null;
			return getSession().get(parent[0]);
		}
		if (!(parentNode instanceof Element))
			throw new IllegalStateException("Parent is not an element");
		return new DomContent(this, (Element) parentNode);
	}

	@Override
	public Content add(QName name, QName... classes) {
		// TODO consider classes
		Document document = this.element.getOwnerDocument();
		String namespaceUriOrNull = XMLConstants.NULL_NS_URI.equals(name.getNamespaceURI()) ? null
				: name.getNamespaceURI();
		String prefixToUse = registerPrefixIfNeeded(name);
		Element child = document.createElementNS(namespaceUriOrNull,
				namespaceUriOrNull == null ? name.getLocalPart() : prefixToUse + ":" + name.getLocalPart());
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

	@SuppressWarnings("unchecked")
	@Override
	public <A> A adapt(Class<A> clss) throws IllegalArgumentException {
		if (CharBuffer.class.isAssignableFrom(clss)) {
			String textContent = element.getTextContent();
			CharBuffer buf = CharBuffer.wrap(textContent);
			return (A) buf;
		} else if (Source.class.isAssignableFrom(clss)) {
			DOMSource source = new DOMSource(element);
			return (A) source;
		}
		return super.adapt(clss);
	}

	@SuppressWarnings("unchecked")
	public <A> CompletableFuture<A> write(Class<A> clss) {
		if (String.class.isAssignableFrom(clss)) {
			CompletableFuture<String> res = new CompletableFuture<>();
			res.thenAccept((s) -> {
				getSession().notifyModification(this);
				element.setTextContent(s);
			});
			return (CompletableFuture<A>) res;
		} else if (Source.class.isAssignableFrom(clss)) {
			CompletableFuture<Source> res = new CompletableFuture<>();
			res.thenAccept((source) -> {
				try {
					Transformer transformer = provider.getTransformerFactory().newTransformer();
					DocumentFragment documentFragment = element.getOwnerDocument().createDocumentFragment();
					DOMResult result = new DOMResult(documentFragment);
					transformer.transform(source, result);
					// Node parentNode = element.getParentNode();
					Element resultElement = (Element) documentFragment.getFirstChild();
					QName resultName = toQName(resultElement);
					if (!resultName.equals(getName()))
						throw new IllegalArgumentException(resultName + "+ is not compatible with " + getName());

					// attributes
					NamedNodeMap attrs = resultElement.getAttributes();
					for (int i = 0; i < attrs.getLength(); i++) {
						Attr attr2 = (Attr) element.getOwnerDocument().importNode(attrs.item(i), true);
						element.getAttributes().setNamedItem(attr2);
					}

					// Move all the children
					while (element.hasChildNodes()) {
						element.removeChild(element.getFirstChild());
					}
					while (resultElement.hasChildNodes()) {
						element.appendChild(resultElement.getFirstChild());
					}
//					parentNode.replaceChild(resultNode, element);
//					element = (Element)resultNode;

				} catch (DOMException | TransformerException e) {
					throw new RuntimeException("Cannot write to element", e);
				}
			});
			return (CompletableFuture<A>) res;
		}
		return super.write(clss);
	}

	@Override
	public int getSiblingIndex() {
		Node curr = element.getPreviousSibling();
		int count = 1;
		while (curr != null) {
			if (curr instanceof Element) {
				if (Objects.equals(curr.getNamespaceURI(), element.getNamespaceURI())
						&& Objects.equals(curr.getLocalName(), element.getLocalName())) {
					count++;
				}
			}
			curr = curr.getPreviousSibling();
		}
		return count;
	}

	/*
	 * TYPING
	 */
	@Override
	public List<QName> getContentClasses() {
		List<QName> res = new ArrayList<>();
		if (isLocalRoot()) {
			String mountPath = provider.getMountPath();
			if (mountPath != null) {
				Content mountPoint = getSession().getMountPoint(mountPath);
				res.addAll(mountPoint.getContentClasses());
			}
		} else {
			res.add(getName());
		}
		return res;
	}

	@Override
	public void addContentClasses(QName... contentClass) {
		if (isLocalRoot()) {
			String mountPath = provider.getMountPath();
			if (mountPath != null) {
				Content mountPoint = getSession().getMountPoint(mountPath);
				mountPoint.addContentClasses(contentClass);
			}
		} else {
			super.addContentClasses(contentClass);
		}
	}

	/*
	 * MOUNT MANAGEMENT
	 */
	@Override
	public ProvidedContent getMountPoint(String relativePath) {
		// FIXME use qualified names
		Element childElement = (Element) element.getElementsByTagName(relativePath).item(0);
		// TODO check that it is a mount
		return new DomContent(this, childElement);
	}

	@Override
	public DomContentProvider getProvider() {
		return provider;
	}

}
