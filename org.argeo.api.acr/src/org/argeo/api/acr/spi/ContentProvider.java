package org.argeo.api.acr.spi;

import java.util.Iterator;
import java.util.Spliterator;

import javax.xml.namespace.NamespaceContext;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.search.BasicSearch;

public interface ContentProvider extends NamespaceContext {

	ProvidedContent get(ProvidedSession session, String relativePath);

	boolean exists(ProvidedSession session, String relativePath);

	String getMountPath();

	/*
	 * NAMESPACE CONTEXT
	 */
	@Override
	default String getPrefix(String namespaceURI) {
		Iterator<String> prefixes = getPrefixes(namespaceURI);
		return prefixes.hasNext() ? prefixes.next() : null;
	}

	default Spliterator<Content> search(ProvidedSession session, BasicSearch search, String relPath) {
		throw new UnsupportedOperationException();
	}

//	default ContentName parsePrefixedName(String nameWithPrefix) {
//		return NamespaceUtils.parsePrefixedName(this, nameWithPrefix);
//	}
//
//	default String toPrefixedName(QName name) {
//		return NamespaceUtils.toPrefixedName(this, name);
//	}

}
