package org.argeo.cms.acr;

import java.util.Objects;

import org.argeo.api.acr.Content;
import org.argeo.api.acr.spi.ProvidedContent;

/** A content within a CMS system. */
public interface CmsContent extends ProvidedContent {

	/**
	 * Split a path (with {@link Content#PATH_SEPARATOR} separator) in an array of
	 * length 2, the first part being the parent path (which could be either
	 * absolute or relative), the second one being the last segment, (guaranteed to
	 * be without a '/').
	 */
	static String[] getParentPath(String path) {
		if (path == null)
			throw new IllegalArgumentException("Path cannot be null");
		if (path.length() == 0)
			throw new IllegalArgumentException("Path cannot be empty");
		ContentUtils.checkDoubleSlash(path);
		int parentIndex = path.lastIndexOf(PATH_SEPARATOR);
		if (parentIndex == path.length() - 1) {// trailing '/'
			path = path.substring(0, path.length() - 1);
			parentIndex = path.lastIndexOf(PATH_SEPARATOR);
		}

		if (parentIndex == -1) // no '/'
			return new String[] { "", path };

		return new String[] { parentIndex != 0 ? path.substring(0, parentIndex) : ContentUtils.PATH_SEPARATOR_STRING,
				path.substring(parentIndex + 1) };
	}

	/**
	 * Constructs a relative path between a base path and a given path.
	 * 
	 * @throws IllegalArgumentException if the base path is not an ancestor of the
	 *                                  path
	 */
	static String relativize(String basePath, String path) throws IllegalArgumentException {
		Objects.requireNonNull(basePath);
		Objects.requireNonNull(path);
		if (!path.startsWith(basePath))
			throw new IllegalArgumentException(basePath + " is not an ancestor of " + path);
		String relativePath = path.substring(basePath.length());
		if (relativePath.length() > 0 && relativePath.charAt(0) == PATH_SEPARATOR)
			relativePath = relativePath.substring(1);
		return relativePath;
	}

}
