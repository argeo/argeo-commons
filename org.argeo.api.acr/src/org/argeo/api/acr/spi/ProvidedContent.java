package org.argeo.api.acr.spi;

import org.argeo.api.acr.Content;

/** A {@link Content} implementation. */
public interface ProvidedContent extends Content {
	final static String ROOT_PATH = "/";

	ProvidedSession getSession();

	ContentProvider getProvider();

	int getDepth();

	/**
	 * Whether this is the root node of the related repository. Default checks
	 * whether <code>{@link #getDepth()} == 0</code>, but it can be optimised by
	 * implementations.
	 */
	default boolean isRoot() {
		return getDepth() == 0;
	}

	/**
	 * An opaque ID which is guaranteed to uniquely identify this content within the
	 * session return by {@link #getSession()}. Typically used for UI.
	 */
	String getSessionLocalId();

	default ProvidedContent getMountPoint(String relativePath) {
		throw new UnsupportedOperationException("This content doe not support mount");
	}

	default ProvidedContent getContent(String path) {
		Content fileNode;
		if (path.startsWith(ROOT_PATH)) {// absolute
			fileNode = getSession().get(path);
		} else {// relative
			String absolutePath = getPath() + '/' + path;
			fileNode = getSession().get(absolutePath);
		}
		return (ProvidedContent) fileNode;
	}

	/*
	 * ACCESS
	 */
	/** Whether the session has the right to access the parent. */
	default boolean isParentAccessible() {
		return true;
	}

}
