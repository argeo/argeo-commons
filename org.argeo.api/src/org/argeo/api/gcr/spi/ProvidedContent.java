package org.argeo.api.gcr.spi;

import org.argeo.api.gcr.Content;

public interface ProvidedContent extends Content {
	ProvidedSession getSession();

	ContentProvider getProvider();
}
