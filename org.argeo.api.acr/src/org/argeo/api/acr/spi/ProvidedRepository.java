package org.argeo.api.acr.spi;

import org.argeo.api.acr.ContentRepository;

public interface ProvidedRepository extends ContentRepository {
	public void registerTypes(String prefix, String namespaceURI, String schemaSystemId);
}
