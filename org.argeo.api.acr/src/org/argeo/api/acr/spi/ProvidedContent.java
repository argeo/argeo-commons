package org.argeo.api.acr.spi;

import java.util.Optional;

import org.argeo.api.acr.Content;

/** A {@link Content} implementation. */
public interface ProvidedContent extends Content {
	/** The related {@link ProvidedSession}. */
	ProvidedSession getSession();

	/** The {@link ContentProvider} this {@link Content} belongs to. */
	ContentProvider getProvider();

	/** Depth relative to the root of the repository. */
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

	/**
	 * The {@link Content} within the same {@link ContentProvider} which can be used
	 * to mount another {@link ContentProvider}.
	 */
	default ProvidedContent getMountPoint(String relativePath) {
		throw new UnsupportedOperationException("This content doe not support mount");
	}

	@Override
	default Optional<Content> getContent(String path) {
		String absolutePath;
		if (path.startsWith(ROOT_PATH)) {// absolute
			absolutePath = path;
		} else {// relative
			absolutePath = getPath() + PATH_SEPARATOR + path;
		}
		return getSession().exists(absolutePath) ? Optional.of(getSession().get(absolutePath)) : Optional.empty();
	}

	/*
	 * ACCESS
	 */
	/** Whether the session has the right to access the parent. */
	default boolean isParentAccessible() {
		return true;
	}

	/** Whether the related session can open this content for edit. */
	default boolean canEdit() {
		return false;
	}
}
