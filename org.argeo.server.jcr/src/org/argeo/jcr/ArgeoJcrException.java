package org.argeo.jcr;

/** Argeo JCR specific exceptions. */
public class ArgeoJcrException extends RuntimeException {
	private static final long serialVersionUID = -1941940005390084331L;

	public ArgeoJcrException(String message, Throwable cause) {
		super(message, cause);
	}

	public ArgeoJcrException(String message) {
		super(message);
	}

}
