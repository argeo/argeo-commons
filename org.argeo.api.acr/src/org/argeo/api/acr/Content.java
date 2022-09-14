package org.argeo.api.acr;

import static org.argeo.api.acr.NamespaceUtils.unqualified;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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

	Class<?> getType(QName key);

	boolean isMultiple(QName key);

	<A> List<A> getMultiple(QName key, Class<A> clss);

	/*
	 * ATTRIBUTES OPERATION HELPERS
	 */
	default boolean containsKey(QNamed key) {
		return containsKey(key.qName());
	}

	default <A> Optional<A> get(QNamed key, Class<A> clss) {
		return get(key.qName(), clss);
	}

	default Object get(QNamed key) {
		return get(key.qName());
	}

	default Object put(QNamed key, Object value) {
		return put(key.qName(), value);
	}

	default Object remove(QNamed key) {
		return remove(key.qName());
	}

	// TODO do we really need the helpers below?

	default Object get(String key) {
		return get(unqualified(key));
	}

	default Object put(String key, Object value) {
		return put(unqualified(key), value);
	}

	default Object remove(String key) {
		return remove(unqualified(key));
	}

	@SuppressWarnings("unchecked")
	default <A> List<A> getMultiple(QName key) {
		Class<A> type;
		try {
			type = (Class<A>) getType(key);
		} catch (ClassCastException e) {
			throw new IllegalArgumentException("Requested type is not the default type");
		}
		List<A> res = getMultiple(key, type);
		return res;
//		if (res == null)
//			return null;
//		else {
//			if (res.isEmpty())
//				throw new IllegalStateException("Metadata " + key + " is not availabel as list of type " + type);
//			return res.get();
//		}
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
		return add(unqualified(name), classes);
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

	default boolean hasContentClass(QNamed contentClass) {
		return hasContentClass(contentClass.qName());
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

	default boolean hasChild(QNamed name) {
		return hasChild(name.qName());
	}

	default Content anyOrAddChild(QName name, QName... classes) {
		Content child = anyChild(name);
		if (child != null)
			return child;
		return this.add(name, classes);
	}

	default Content anyOrAddChild(String name, QName... classes) {
		return anyOrAddChild(unqualified(name), classes);
	}

	/** Any child with this name, or null if there is none */
	default Content anyChild(QName name) {
		for (Content child : this) {
			if (child.getName().equals(name))
				return child;
		}
		return null;
	}

	default List<Content> children(QName name) {
		List<Content> res = new ArrayList<>();
		for (Content child : this) {
			if (child.getName().equals(name))
				res.add(child);
		}
		return res;
	}

	default Optional<Content> soleChild(QName name) {
		List<Content> res = children(name);
		if (res.isEmpty())
			return Optional.empty();
		if (res.size() > 1)
			throw new IllegalStateException(this + " has multiple children with name " + name);
		return Optional.of(res.get(0));
	}

	default Content child(QName name) {
		return soleChild(name).orElseThrow();
	}

	default Content child(QNamed name) {
		return child(name.qName());
	}

	/*
	 * ATTR AS STRING
	 */
	default String attr(QName key) {
		// TODO check String type?
		Object obj = get(key);
		if (obj == null)
			return null;
		return obj.toString();
	}

	default String attr(QNamed key) {
		return attr(key.qName());
	}

	default String attr(String key) {
		return attr(unqualified(key));
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
