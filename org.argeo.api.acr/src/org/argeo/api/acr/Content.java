package org.argeo.api.acr;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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

	<A> Optional<A> get(QName key, Class<A> clss);

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

	Class<?> getType(QName key);

	boolean isMultiple(QName key);

	<A> Optional<List<A>> getMultiple(QName key, Class<A> clss);

	@SuppressWarnings("unchecked")
	default <A> List<A> getMultiple(QName key) {
		Class<A> type;
		try {
			type = (Class<A>) getType(key);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Requested type is not the default type");
		}
		Optional<List<A>> res = getMultiple(key, type);
		if (res == null)
			return null;
		else {
			if (res.isEmpty())
				throw new IllegalStateException("Metadata " + key + " is not availabel as list of type " + type);
			return res.get();
		}
	}

	/*
	 * CONTENT OPERATIONS
	 */
//	default CompletionStage<Content> edit(Consumer<Content> work) {
//		return CompletableFuture.supplyAsync(() -> {
//			work.accept(this);
//			return this;
//		}).minimalCompletionStage();
//	}

	Content add(QName name, QName... classes);

	default Content add(String name, QName... classes) {
		if (name.indexOf(':') >= 0)
			throw new IllegalArgumentException("Name " + name + " has a prefix");
		return add(new QName(XMLConstants.NULL_NS_URI, name, XMLConstants.DEFAULT_NS_PREFIX), classes);
	}

	void remove();

	/*
	 * TYPING
	 */
	List<QName> getContentClasses();

	default void addContentClasses(QName... contentClass) {
		throw new UnsupportedOperationException("Adding content classes to " + getPath() + " is not supported");
	}

	/** AND */
	default boolean isContentClass(QName... contentClass) {
		List<QName> contentClasses = getContentClasses();
		for (QName cClass : contentClass) {
			if (!contentClasses.contains(cClass))
				return false;
		}
		return true;
	}

	/** OR */
	default boolean hasContentClass(QName... contentClass) {
		List<QName> contentClasses = getContentClasses();
		for (QName cClass : contentClass) {
			if (contentClasses.contains(cClass))
				return true;
		}
		return false;
	}

	/*
	 * SIBLINGS
	 */

	default int getSiblingIndex() {
		return 1;
	}

	/*
	 * DEFAULT METHODS
	 */
	default <A> A adapt(Class<A> clss) {
		throw new UnsupportedOperationException("Cannot adapt content " + this + " to " + clss.getName());
	}

	default <C extends Closeable> C open(Class<C> clss) throws IOException {
		throw new UnsupportedOperationException("Cannot open content " + this + " as " + clss.getName());
	}

	default <A> CompletableFuture<A> write(Class<A> clss) {
		throw new UnsupportedOperationException("Cannot write content " + this + " as " + clss.getName());
	}

	/*
	 * CHILDREN
	 */

	default boolean hasChild(QName name) {
		for (Content child : this) {
			if (child.getName().equals(name))
				return true;
		}
		return false;
	}

	default Content anyOrAddChild(QName name, QName... classes) {
		Content child = anyChild(name);
		if (child != null)
			return child;
		return this.add(name, classes);
	}

	/** Any child with this name, or null if there is none */
	default Content anyChild(QName name) {
		for (Content child : this) {
			if (child.getName().equals(name))
				return child;
		}
		return null;
	}

	/*
	 * CONVENIENCE METHODS
	 */
	default String attr(String key) {
		Object obj = get(key);
		if (obj == null)
			return null;
		return obj.toString();

	}

	default String attr(QName key) {
		Object obj = get(key);
		if (obj == null)
			return null;
		return obj.toString();

	}
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
