package org.argeo.api.acr;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** A hierarchical structure of unnamed mappings. */
public interface StructuredData<KEY, VALUE, CHILD> extends Map<KEY, VALUE>, Iterable<CHILD> {
	/*
	 * ATTRIBUTES OPERATIONS
	 */

	<A> Optional<A> get(KEY key, Class<A> clss);

	Class<? extends VALUE> getType(KEY key);

	default boolean isMultiple(KEY key) {
		return false;
	}

	default <A> List<A> getMultiple(KEY key, Class<A> clss) {
		Optional<A> value = get(key, clss);
		return value.isEmpty() ? Collections.emptyList() : Collections.singletonList(value.get());
	}

	/*
	 * DEFAULT METHODS
	 */
	default <A> A adapt(Class<A> clss) {
		throw new UnsupportedOperationException("Cannot adapt content " + this + " to " + clss.getName());
	}
}
