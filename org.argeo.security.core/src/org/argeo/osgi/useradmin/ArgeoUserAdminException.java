package org.argeo.osgi.useradmin;

import org.osgi.service.useradmin.UserAdmin;

/**
 * Exceptions related to Argeo's implementation of OSGi {@link UserAdmin}
 * service.
 */
public class ArgeoUserAdminException extends RuntimeException {
	private static final long serialVersionUID = 1419352360062048603L;

	public ArgeoUserAdminException(String message) {
		super(message);
	}

	public ArgeoUserAdminException(String message, Throwable e) {
		super(message, e);
	}
}
