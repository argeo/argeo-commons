package org.argeo.api.acr.spi;

import org.argeo.api.acr.Content;

public interface ProvidedContent extends Content {
	ProvidedSession getSession();

	ContentProvider getProvider();
}
