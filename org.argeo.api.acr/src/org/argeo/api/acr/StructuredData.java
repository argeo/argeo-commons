package org.argeo.api.acr;

import java.util.Map;

/** A hierarchical structure of unnamed mappings. */
public interface StructuredData<KEY, VALUE, CHILD> extends Map<KEY, VALUE>, Iterable<CHILD> {

}
