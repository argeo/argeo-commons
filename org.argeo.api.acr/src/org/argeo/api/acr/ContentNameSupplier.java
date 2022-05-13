package org.argeo.api.acr;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

public interface ContentNameSupplier extends Supplier<ContentName>, NamespaceContext {
	String name();

	String getNamespaceURI();

	String getDefaultPrefix();

	@Override
	default ContentName get() {
		return toContentName();
	}

	default ContentName toContentName() {
		CompositeString cs = new CompositeString(name());
		String camlName = cs.toStringCaml(false);
		return new ContentName(getNamespaceURI(), camlName, this);
	}

//	default String getNamespaceURI() {
//		return XMLConstants.NULL_NS_URI;
//	}
//
//	default String getDefaultPrefix() {
//		return XMLConstants.DEFAULT_NS_PREFIX;
//	}

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
		String namespaceURI = NamespaceUtils.getStandardNamespaceURI(prefix);
		if (namespaceURI != null)
			return namespaceURI;
		if (prefix.equals(getDefaultPrefix()))
			return getNamespaceURI();
		return XMLConstants.NULL_NS_URI;
	}

	@Override
	default String getPrefix(String namespaceURI) {
		String prefix = NamespaceUtils.getStandardPrefix(namespaceURI);
		if (prefix != null)
			return prefix;
		if (namespaceURI.equals(getNamespaceURI()))
			return getDefaultPrefix();
		return null;
	}

	@Override
	default Iterator<String> getPrefixes(String namespaceURI) {
		Iterator<String> it = NamespaceUtils.getStandardPrefixes(namespaceURI);
		if (it != null)
			return it;
		if (namespaceURI.equals(getNamespaceURI()))
			return Collections.singleton(getDefaultPrefix()).iterator();
		return Collections.emptyIterator();
	}

}
