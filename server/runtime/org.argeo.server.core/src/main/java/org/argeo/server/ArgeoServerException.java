package org.argeo.server;

public class ArgeoServerException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ArgeoServerException() {
	}

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
