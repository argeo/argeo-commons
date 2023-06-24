package org.argeo.api.acr.spi;

import java.util.Iterator;
import java.util.Spliterator;

import javax.xml.namespace.NamespaceContext;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentNotFoundException;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.search.BasicSearch;

/**
 * A prover of {@link Content}, which can be mounted in a
 * {@link ProvidedRepository}.
 */
public interface ContentProvider extends NamespaceContext {

	/**
	 * Return the content at this path, relative to the mount path.
	 * 
	 * @return the content at this relative path, never <code>null</code>
	 * @throws ContentNotFoundException if there is no content at this relative path
	 */
	ProvidedContent get(ProvidedSession session, String relativePath) throws ContentNotFoundException;

	/**
	 * Whether a content exist at his relative path. The default implementation call
	 * {@link #get(ProvidedSession, String)} and check whether a
	 * {@link ContentNotFoundException} is thrown or not. It should be overridden as
	 * soon as there is a mechanism to check existence before actually getting the
	 * content.
	 */
	default boolean exists(ProvidedSession session, String relativePath) {
		try {
			get(session, relativePath);
			return true;
		} catch (ContentNotFoundException e) {
			return false;
		}
	}

	/** The absolute path where this provider is mounted. */
	String getMountPath();

	/**
	 * Search content within this provider. The default implementation throws an
	 * {@link UnsupportedOperationException}.
	 */
	default Spliterator<Content> search(ProvidedSession session, BasicSearch search, String relPath) {
		throw new UnsupportedOperationException();
	}

	/*
	 * EDITION
	 */
	/** Switch this content (and its subtree) to editing mode. */
	default void openForEdit(ProvidedSession session, String relativePath) {
		throw new UnsupportedOperationException();
	}

	/** Switch this content (and its subtree) to frozen mode. */
	default void freeze(ProvidedSession session, String relativePath) {
		throw new UnsupportedOperationException();
	}

	/** Whether this content (and its subtree) are in editing mode. */
	default boolean isOpenForEdit(ProvidedSession session, String relativePath) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Called when an edition cycle is completed. Does nothing by default.
	 * 
	 * @see ContentSession#edit(java.util.function.Consumer)
	 */
	default void persist(ProvidedSession session) {
	}

	/*
	 * NAMESPACE CONTEXT
	 */
	@Override
	default String getPrefix(String namespaceURI) {
		Iterator<String> prefixes = getPrefixes(namespaceURI);
		return prefixes.hasNext() ? prefixes.next() : null;
	}

}
