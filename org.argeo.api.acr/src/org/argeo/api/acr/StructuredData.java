package org.argeo.api.acr;

import java.util.Map;

/** A hierarchical structure of unnamed mappings. */
public interface StructuredData<KEY, VALUE, CHILD> extends Map<KEY, VALUE>, Iterable<CHILD> {
	/*
	 * DEFAULT METHODS
	 */
	default <A> A adapt(Class<A> clss) {
		throw new UnsupportedOperationException("Cannot adapt content " + this + " to " + clss.getName());
	}
}
