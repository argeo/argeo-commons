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
	/** The base of a repository path. */
	String ROOT_PATH = "/";

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
	}

	/*
	 * CONTENT OPERATIONS
	 */
	/** Adds a new empty {@link Content} to this {@link Content}. */
	Content add(QName name, QName... contentClass);

	default Content add(QName name, QNamed... contentClass) {
		return add(name, toQNames(contentClass));
	}

	/**
	 * Adds a new {@link Content} to this {@link Content}, setting the provided
	 * attributes. The provided attributes can be used as hints by the
	 * implementation. In particular, setting {@link DName#getcontenttype} will
	 * imply that this content has a file semantic.
	 */
	default Content add(QName name, Map<QName, Object> attrs, QName... classes) {
		Content child = add(name, classes);
		putAll(attrs);
		return child;
	}

	default Content add(String name, QName... classes) {
		return add(unqualified(name), classes);
	}

	default Content add(String name, Map<QName, Object> attrs, QName... classes) {
		return add(unqualified(name), attrs, classes);
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

	/** AND */
	default boolean isContentClass(QNamed... contentClass) {
		return isContentClass(toQNames(contentClass));
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

	/** OR */
	default boolean hasContentClass(QNamed... contentClass) {
		return hasContentClass(toQNames(contentClass));
	}

	static QName[] toQNames(QNamed... names) {
		QName[] res = new QName[names.length];
		for (int i = 0; i < names.length; i++)
			res[i] = names[i].qName();
		return res;
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

	default List<Content> children(QNamed name) {
		return children(name.qName());
	}

	default Optional<Content> soleChild(QNamed name) {
		return soleChild(name.qName());
	}

	default Optional<Content> soleChild(QName name) {
		List<Content> res = children(name);
		if (res.isEmpty())
			return Optional.empty();
		if (res.size() > 1)
			throw new IllegalStateException(this + " has multiple children with name " + name);
		return Optional.of(res.get(0));
	}

	default Content soleOrAddChild(QName name, QName... classes) {
		return soleChild(name).orElseGet(() -> this.add(name, classes));
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
	/**
	 * Convenience method returning an attribute as a {@link String}.
	 * 
	 * @param key the attribute name
	 * @return the attribute value as a {@link String} or <code>null</code>.
	 * 
	 * @see Object#toString()
	 */
	default String attr(QName key) {
		return get(key, String.class).orElse(null);
	}

	/**
	 * Convenience method returning an attribute as a {@link String}.
	 * 
	 * @param key the attribute name
	 * @return the attribute value as a {@link String} or <code>null</code>.
	 * 
	 * @see Object#toString()
	 */
	default String attr(QNamed key) {
		return attr(key.qName());
	}

	/**
	 * Convenience method returning an attribute as a {@link String}.
	 * 
	 * @param key the attribute name
	 * @return the attribute value as a {@link String} or <code>null</code>.
	 * 
	 * @see Object#toString()
	 */
	default String attr(String key) {
		return attr(unqualified(key));
	}

	/*
	 * CONTEXT
	 */
	/**
	 * A content within this repository
	 * 
	 * @param path either an absolute path or a path relative to this content
	 */
	Optional<Content> getContent(String path);

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
