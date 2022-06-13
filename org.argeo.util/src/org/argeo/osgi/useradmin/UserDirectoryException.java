package org.argeo.osgi.useradmin;

import org.osgi.service.useradmin.UserAdmin;

/**
 * Exceptions related to Argeo's implementation of OSGi {@link UserAdmin}
 * service.
 */
@Deprecated
public class UserDirectoryException extends RuntimeException {
	private static final long serialVersionUID = 1419352360062048603L;

	public UserDirectoryException(String message) {
		super(message);
	}

	public UserDirectoryException(String message, Throwable e) {
		super(message, e);
	}
}
