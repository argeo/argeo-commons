package org.argeo.api.acr.spi;

import java.net.URL;

/** A namespace and its default prefix, possibly with a schema definition. */
public interface ContentNamespace {
	String getDefaultPrefix();

	String getNamespaceURI();

	URL getSchemaResource();

}
