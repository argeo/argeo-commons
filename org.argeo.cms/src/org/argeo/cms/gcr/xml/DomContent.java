package org.argeo.cms.gcr.xml;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.argeo.api.gcr.Content;
import org.argeo.api.gcr.ContentSystemProvider;
import org.argeo.api.gcr.spi.AbstractContent;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

public class DomContent extends AbstractContent implements Content {

	private final DomContentSession contentSession;
	private final Element element;

//	private String text = null;
	private Boolean hasText = null;

	public DomContent(DomContentSession contentSession, Element element) {
		this.contentSession = contentSession;
		this.element = element;
	}

	@Override
	public Iterator<Content> iterator() {
		NodeList nodeList = element.getChildNodes();
		return new ElementIterator(contentSession, nodeList);
	}

	@Override
	public String getName() {
		return element.getNodeName();
	}

	@Override
	public Iterable<String> keys() {
		// TODO implement an iterator?
		Set<String> result = new HashSet<>();
		NamedNodeMap attributes = element.getAttributes();
		for (int i = 0; i < attributes.getLength(); i++) {
			Attr attr = (Attr) attributes.item(i);
			String attrName = attr.getNodeName();
			result.add(attrName);
		}
		return result;
	}

	@Override
	public <A> A get(String key, Class<A> clss) {
		if (element.hasAttribute(key)) {
			String value = element.getAttribute(key);
			if (clss.isAssignableFrom(String.class))
				return (A) value;
			else
				throw new IllegalArgumentException();
		} else
			return null;
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
				Text text =(Text) node;
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

}
