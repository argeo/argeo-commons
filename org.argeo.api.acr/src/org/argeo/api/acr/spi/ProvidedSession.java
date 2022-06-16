package org.argeo.api.acr.spi;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentSession;

/** A {@link ContentSession} implementation. */
public interface ProvidedSession extends ContentSession {
	ProvidedRepository getRepository();

	CompletionStage<ProvidedSession> onClose();

	Content getMountPoint(String path);

	boolean isEditing();

	void notifyModification(ProvidedContent content);

	UUID getUuid();

	Content getSessionRunDir();

	/*
	 * NAMESPACE CONTEXT
	 */

	@Override
	default String getPrefix(String namespaceURI) {
		Iterator<String> prefixes = getPrefixes(namespaceURI);
		return prefixes.hasNext() ? prefixes.next() : null;
	}

//	/** @return the bound namespace or null if not found */
//	String findNamespace(String prefix);
//
//	// TODO find the default prefix?
//	Set<String> findPrefixes(String namespaceURI);
//
//	/** To be overridden for optimisation, as it will be called a lot */
//	default String findPrefix(String namespaceURI) {
//		Set<String> prefixes = findPrefixes(namespaceURI);
//		if (prefixes.isEmpty())
//			return null;
//		return prefixes.iterator().next();
//	}

//	@Override
//	default String getNamespaceURI(String prefix) {
//		String namespaceURI = NamespaceUtils.getStandardNamespaceURI(prefix);
//		if (namespaceURI != null)
//			return namespaceURI;
//		if (XMLConstants.DEFAULT_NS_PREFIX.equals(prefix))
//			return XMLConstants.NULL_NS_URI;
//		namespaceURI = findNamespace(prefix);
//		if (namespaceURI != null)
//			return namespaceURI;
//		return XMLConstants.NULL_NS_URI;
//	}
//
//	@Override
//	default String getPrefix(String namespaceURI) {
//		String prefix = NamespaceUtils.getStandardPrefix(namespaceURI);
//		if (prefix != null)
//			return prefix;
//		if (XMLConstants.NULL_NS_URI.equals(namespaceURI))
//			return XMLConstants.DEFAULT_NS_PREFIX;
//		return findPrefix(namespaceURI);
//	}
//
//	@Override
//	default Iterator<String> getPrefixes(String namespaceURI) {
//		Iterator<String> standard = NamespaceUtils.getStandardPrefixes(namespaceURI);
//		if (standard != null)
//			return standard;
//		if (XMLConstants.NULL_NS_URI.equals(namespaceURI))
//			return Collections.singleton(XMLConstants.DEFAULT_NS_PREFIX).iterator();
//		Set<String> prefixes = findPrefixes(namespaceURI);
//		assert prefixes != null;
//		return prefixes.iterator();
//	}

}
