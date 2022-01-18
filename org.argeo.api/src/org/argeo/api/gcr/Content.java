package org.argeo.api.gcr;

import java.util.Map;

/**
 * A semi-structured content, with attributes, within a hierarchical structure.
 */
public interface Content extends Iterable<Content>, Map<String, Object> {

	String getName();

	String getPath();

	Content getParent();

	/*
	 * ATTRIBUTES OPERATIONS
	 */

	<A> A get(String key, Class<A> clss) throws IllegalArgumentException;

	/*
	 * CONTENT OPERATIONS
	 */
	Content add(String name, ContentName... classes);

	void remove();

	/*
	 * DEFAULT METHODS
	 */
	default <A> A adapt(Class<A> clss) throws IllegalArgumentException {
		throw new IllegalArgumentException("Cannot adapt content " + this + " to " + clss.getName());
	}

	default <C extends AutoCloseable> C open(Class<C> clss) throws Exception, IllegalArgumentException {
		throw new IllegalArgumentException("Cannot open content " + this + " as " + clss.getName());
	}

	/*
	 * CONVENIENCE METHODS
	 */
	default String attr(String key) {
		return get(key, String.class);
	}

	default String attr(Object key) {
		return key != null ? attr(key.toString()) : attr(null);
	}

	default <A> A get(Object key, Class<A> clss) {
		return key != null ? get(key.toString(), clss) : get(null, clss);
	}

	/*
	 * EXPERIMENTAL UNSUPPORTED
	 */
	default boolean hasText() {
		return false;
	}

	default String getText() {
		throw new UnsupportedOperationException();
	}

}
