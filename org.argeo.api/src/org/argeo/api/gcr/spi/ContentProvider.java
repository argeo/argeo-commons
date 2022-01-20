package org.argeo.api.gcr.spi;

import org.argeo.api.gcr.Content;

public interface ContentProvider {

	Content get(ProvidedSession session, String mountPath, String relativePath);

}
