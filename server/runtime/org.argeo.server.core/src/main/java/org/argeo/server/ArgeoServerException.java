package org.argeo.server;

import org.argeo.ArgeoException;

/** @deprecated use {@link ArgeoException} instead */
public class ArgeoServerException extends ArgeoException {
	private static final long serialVersionUID = 1L;

	public ArgeoServerException(String message) {
		super(message);
	}

	public ArgeoServerException(Throwable cause) {
		super(cause);
	}

	public ArgeoServerException(String message, Throwable cause) {
		super(message, cause);
	}

}
