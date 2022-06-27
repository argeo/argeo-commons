package org.argeo.cms.acr.xml;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.CrName;
import org.argeo.api.acr.spi.ProvidedSession;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

class ElementIterator implements Iterator<Content> {
	private DomContent parent;
	private final ProvidedSession session;
	private final DomContentProvider provider;
	private final NodeList nodeList;

	private int currentIndex;
	private final int length;
	private Element nextElement = null;

	public ElementIterator(DomContent parent, ProvidedSession session, DomContentProvider provider, NodeList nodeList) {
		this.parent = parent;
		this.session = session;
		this.provider = provider;
		this.nodeList = nodeList;

		this.length = nodeList.getLength();
		this.currentIndex = 0;
		this.nextElement = findNext();
	}

	private Element findNext() {
		while (currentIndex < length) {
			Node node = nodeList.item(currentIndex);
			if (node instanceof Element) {
				return (Element) node;
			}
			currentIndex++;
		}
		return null;
	}

	@Override
	public boolean hasNext() {
		return nextElement != null;
	}

	@Override
	public Content next() {
		if (nextElement == null)
			throw new NoSuchElementException();
		Content result;
		String isMount = nextElement.getAttributeNS(CrName.CR_NAMESPACE_URI, CrName.mount.qName().getLocalPart());
		if (isMount.equals("true")) {
			result = session.get(parent.getPath() + '/' + nextElement.getTagName());
		}

		else {
			result = new DomContent(session, provider, nextElement);
		}
		currentIndex++;
		nextElement = findNext();
		return result;
	}

}
