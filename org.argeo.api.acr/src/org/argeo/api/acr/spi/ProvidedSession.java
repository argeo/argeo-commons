package org.argeo.api.acr.spi;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentSession;
import org.argeo.api.acr.RuntimeNamespaceContext;

/** A {@link ContentSession} implementation. */
public interface ProvidedSession extends ContentSession {
	ProvidedRepository getRepository();

	CompletionStage<ProvidedSession> onClose();

	Content getMountPoint(String path);

	boolean isEditing();

	void notifyModification(ProvidedContent content);

	UUID uuid();

//	Content getSessionRunDir();

	/*
	 * NAMESPACE CONTEXT
	 */

	@Override
	default String getPrefix(String namespaceURI) {
		return RuntimeNamespaceContext.getNamespaceContext().getPrefix(namespaceURI);
	}

	@Override
	default String getNamespaceURI(String prefix) {
		return RuntimeNamespaceContext.getNamespaceContext().getNamespaceURI(prefix);
	}

	@Override
	default Iterator<String> getPrefixes(String namespaceURI) {
		return RuntimeNamespaceContext.getNamespaceContext().getPrefixes(namespaceURI);
	}
}
