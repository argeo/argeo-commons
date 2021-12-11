package org.argeo.api.gcr;

import java.util.Map;

public interface Content extends Iterable<Content>, Map<String, Object> {

	String getName();

//	Iterable<String> keys();

	<A> A get(String key, Class<A> clss);

	ContentSession getSession();

	/*
	 * DEFAULT METHODS
	 */
	default <A> A adapt(Class<A> clss) {
		return null;
	}

	/*
	 * CONVENIENCE METHODS
	 */
	default String attr(String key) {
		return get(key, String.class);
	}

	default String attr(Enum<?> key) {
		return attr(key.name());
	}

	default <A> A get(Enum<?> key, Class<A> clss) {
		return get(key.name(), clss);
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
