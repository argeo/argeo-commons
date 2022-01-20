package org.argeo.api.gcr;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public interface ContentNameSupplier extends Supplier<ContentName>, NamespaceContext {
	String name();

	@Override
	default ContentName get() {
		return toContentName();
	}

	default ContentName toContentName() {
		CompositeString cs = new CompositeString(name());
		String camlName = cs.toStringCaml(false);
		return new ContentName(getNamespaceURI(), camlName, this);
	}

	default String getNamespaceURI() {
		return XMLConstants.NULL_NS_URI;
	}

	default String getDefaultPrefix() {
		return XMLConstants.DEFAULT_NS_PREFIX;
	}

//	static ContentName toContentName(String namespaceURI, String localName, String prefix) {
//		CompositeString cs = new CompositeString(localName);
//		String camlName = cs.toStringCaml(false);
//		return new ContentName(namespaceURI, camlName, this);
//	}

	/*
	 * NAMESPACE CONTEXT
	 */

	@Override
	default String getNamespaceURI(String prefix) {
		String namespaceURI = getStandardNamespaceURI(prefix);
		if (namespaceURI != null)
			return namespaceURI;
		if (prefix.equals(getDefaultPrefix()))
			return getNamespaceURI();
		return XMLConstants.NULL_NS_URI;
	}

	@Override
	default String getPrefix(String namespaceURI) {
		String prefix = getStandardPrefix(namespaceURI);
		if (prefix != null)
			return prefix;
		if (namespaceURI.equals(getNamespaceURI()))
			return getDefaultPrefix();
		return null;
	}

	@Override
	default Iterator<String> getPrefixes(String namespaceURI) {
		Iterator<String> it = getStandardPrefixes(namespaceURI);
		if (it != null)
			return it;
		if (namespaceURI.equals(getNamespaceURI()))
			return Collections.singleton(getDefaultPrefix()).iterator();
		return Collections.emptyIterator();
	}

	/*
	 * DEFAULT NAMESPACE CONTEXT OPERATIONS as specified in NamespaceContext
	 */
	static String getStandardPrefix(String namespaceURI) {
		if (namespaceURI == null)
			throw new IllegalArgumentException("Namespace URI cannot be null");
		if (XMLConstants.XML_NS_URI.equals(namespaceURI))
			return XMLConstants.XML_NS_PREFIX;
		else if (XMLConstants.XMLNS_ATTRIBUTE_NS_URI.equals(namespaceURI))
			return XMLConstants.XMLNS_ATTRIBUTE;
		return null;
	}

	static Iterator<String> getStandardPrefixes(String namespaceURI) {
		String prefix = ContentNameSupplier.getStandardPrefix(namespaceURI);
		if (prefix == null)
			return null;
		return Collections.singleton(prefix).iterator();
	}

	static String getStandardNamespaceURI(String prefix) {
		if (prefix == null)
			throw new IllegalArgumentException("Prefix cannot be null");
		if (XMLConstants.XML_NS_PREFIX.equals(prefix))
			return XMLConstants.XML_NS_URI;
		else if (XMLConstants.XMLNS_ATTRIBUTE.equals(prefix))
			return XMLConstants.XMLNS_ATTRIBUTE_NS_URI;
		return null;
	}

}
