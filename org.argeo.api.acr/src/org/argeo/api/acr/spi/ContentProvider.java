package org.argeo.api.acr.spi;

import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

import org.argeo.api.acr.Content;

public interface ContentProvider extends NamespaceContext {

	Content get(ProvidedSession session, String mountPath, String relativePath);

	String getMountPath();

	/*
	 * NAMESPACE CONTEXT
	 */
	@Override
	default String getPrefix(String namespaceURI) {
		Iterator<String> prefixes = getPrefixes(namespaceURI);
		return prefixes.hasNext() ? prefixes.next() : null;
	}

//	default ContentName parsePrefixedName(String nameWithPrefix) {
//		return NamespaceUtils.parsePrefixedName(this, nameWithPrefix);
//	}
//
//	default String toPrefixedName(QName name) {
//		return NamespaceUtils.toPrefixedName(this, name);
//	}

}
