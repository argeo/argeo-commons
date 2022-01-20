package org.argeo.api.gcr;

import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

/**
 * A semi-structured content, with attributes, within a hierarchical structure.
 */
public interface Content extends Iterable<Content>, Map<QName, Object> {

	QName getName();

	String getPath();

	Content getParent();

	/*
	 * ATTRIBUTES OPERATIONS
	 */

	<A> A get(QName key, Class<A> clss) throws IllegalArgumentException;

	default Object get(String key) {
		if (key.indexOf(':') >= 0)
			throw new IllegalArgumentException("Name " + key + " has a prefix");
		return get(new QName(XMLConstants.NULL_NS_URI, key, XMLConstants.DEFAULT_NS_PREFIX));
	}

	default Object put(String key, Object value) {
		if (key.indexOf(':') >= 0)
			throw new IllegalArgumentException("Name " + key + " has a prefix");
		return put(new QName(XMLConstants.NULL_NS_URI, key, XMLConstants.DEFAULT_NS_PREFIX), value);
	}

	default Object remove(String key) {
		if (key.indexOf(':') >= 0)
			throw new IllegalArgumentException("Name " + key + " has a prefix");
		return remove(new QName(XMLConstants.NULL_NS_URI, key, XMLConstants.DEFAULT_NS_PREFIX));
	}

	/*
	 * CONTENT OPERATIONS
	 */
	Content add(QName name, QName... classes);

	default Content add(String name, QName... classes) {
		if (name.indexOf(':') >= 0)
			throw new IllegalArgumentException("Name " + name + " has a prefix");
		return add(new QName(XMLConstants.NULL_NS_URI, name, XMLConstants.DEFAULT_NS_PREFIX), classes);
	}

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
//	default String attr(String key) {
//		return get(key, String.class);
//	}
//
//	default String attr(Object key) {
//		return key != null ? attr(key.toString()) : attr(null);
//	}
//
//	default <A> A get(Object key, Class<A> clss) {
//		return key != null ? get(key.toString(), clss) : get(null, clss);
//	}

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
