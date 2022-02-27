package org.argeo.api.acr.spi;

import org.argeo.api.acr.Content;

public interface ContentProvider {

	Content get(ProvidedSession session, String mountPath, String relativePath);

}
