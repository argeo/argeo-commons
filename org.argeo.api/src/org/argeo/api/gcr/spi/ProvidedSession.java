package org.argeo.api.gcr.spi;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

import org.argeo.api.gcr.ContentNameSupplier;
import org.argeo.api.gcr.ContentSession;

public interface ProvidedSession extends ContentSession, NamespaceContext {
	ProvidedRepository getRepository();

	/*
	 * NAMESPACE CONTEXT
	 */
	/** @return the bound namespace or null if not found */
	String findNamespace(String prefix);

	// TODO find the default prefix?
	Set<String> findPrefixes(String namespaceURI);

	/** To be overridden for optimisation, as it will be called a lot */
	default String findPrefix(String namespaceURI) {
		Set<String> prefixes = findPrefixes(namespaceURI);
		if (prefixes.isEmpty())
			return null;
		return prefixes.iterator().next();
	}

	@Override
	default String getNamespaceURI(String prefix) {
		String namespaceURI = ContentNameSupplier.getStandardNamespaceURI(prefix);
		if (namespaceURI != null)
			return namespaceURI;
		if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix))
			return XMLConstants.NULL_NS_URI;
		namespaceURI = findNamespace(prefix);
		if (namespaceURI != null)
			return namespaceURI;
		return XMLConstants.NULL_NS_URI;
	}

	@Override
	default String getPrefix(String namespaceURI) {
		String prefix = ContentNameSupplier.getStandardPrefix(namespaceURI);
		if (prefix != null)
			return prefix;
		if (XMLConstants.NULL_NS_URI.equals(namespaceURI))
			return XMLConstants.DEFAULT_NS_PREFIX;
		return findPrefix(namespaceURI);
	}

	@Override
	default Iterator<String> getPrefixes(String namespaceURI) {
		Iterator<String> standard = ContentNameSupplier.getStandardPrefixes(namespaceURI);
		if (standard != null)
			return standard;
		if (XMLConstants.NULL_NS_URI.equals(namespaceURI))
			return Collections.singleton(XMLConstants.DEFAULT_NS_PREFIX).iterator();
		Set<String> prefixes = findPrefixes(namespaceURI);
		assert prefixes != null;
		return prefixes.iterator();
	}

}
