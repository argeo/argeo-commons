package org.argeo.api.acr.spi;

import org.argeo.api.acr.Content;

/** A {@link Content} implementation. */
public interface ProvidedContent extends Content {
	ProvidedSession getSession();

	ContentProvider getProvider();

	default ProvidedContent getMountPoint(String relativePath) {
		throw new UnsupportedOperationException("This content doe not support mount");
	}
}
