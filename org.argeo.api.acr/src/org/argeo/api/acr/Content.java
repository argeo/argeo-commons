package org.argeo.api.acr;

import static org.argeo.api.acr.NamespaceUtils.unqualified;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.xml.namespace.QName;

/**
 * A semi-structured content, with attributes, within a hierarchical structure
 * whose nodes are named.
 */
public interface Content extends QualifiedData<Content> {
	/** The path separator: '/' */
	char PATH_SEPARATOR = '/';

	/** The base of a repository path. */
	String ROOT_PATH = Character.toString(PATH_SEPARATOR);

	String getPath();

	/** MUST be {@link Content#PATH_SEPARATOR}. */
	default char getPathSeparator() {
		return PATH_SEPARATOR;
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
	 * DEFAULT METHODS
	 */
	default <C extends Closeable> C open(Class<C> clss) throws IOException {
		throw new UnsupportedOperationException("Cannot open content " + this + " as " + clss.getName());
	}

	default <A> CompletableFuture<A> write(Class<A> clss) {
		throw new UnsupportedOperationException("Cannot write content " + this + " as " + clss.getName());
	}

	/*
	 * CHILDREN
	 */
	default Content anyOrAddChild(QName name, QName... classes) {
		Content child = anyChild(name);
		if (child != null)
			return child;
		return this.add(name, classes);
	}

	default Content anyOrAddChild(String name, QName... classes) {
		return anyOrAddChild(unqualified(name), classes);
	}

	default Content soleOrAddChild(QName name, QName... classes) {
		return soleChild(name).orElseGet(() -> this.add(name, classes));
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
