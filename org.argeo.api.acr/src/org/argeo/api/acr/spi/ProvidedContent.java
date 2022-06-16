package org.argeo.api.acr.spi;

import org.argeo.api.acr.Content;

/** A {@link Content} implementation. */
public interface ProvidedContent extends Content {
	ProvidedSession getSession();

	ContentProvider getProvider();

	int getDepth();

	/**
	 * An opaque ID which is guaranteed to uniquely identify this content within the
	 * session return by {@link #getSession()}. Typically used for UI.
	 */
	String getSessionLocalId();

	default ProvidedContent getMountPoint(String relativePath) {
		throw new UnsupportedOperationException("This content doe not support mount");
	}
}
