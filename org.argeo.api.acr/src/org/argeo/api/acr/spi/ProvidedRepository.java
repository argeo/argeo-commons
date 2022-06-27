package org.argeo.api.acr.spi;

import javax.xml.namespace.QName;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.ContentRepository;

/** A {@link ContentRepository} implementation. */
public interface ProvidedRepository extends ContentRepository {
	void registerTypes(String prefix, String namespaceURI, String schemaSystemId);

	ContentProvider getMountContentProvider(Content mountPoint, boolean initialize, QName... types);

	boolean shouldMount(QName... types);

	void addProvider(ContentProvider provider);
}
