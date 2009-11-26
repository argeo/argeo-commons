package org.argeo;

public class ArgeoException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ArgeoException(String message) {
		super(message);
	}

	public ArgeoException(Throwable cause) {
		super(cause);
	}

	public ArgeoException(String message, Throwable cause) {
		super(message, cause);
	}

}
