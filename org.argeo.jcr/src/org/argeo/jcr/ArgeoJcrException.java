package org.argeo.jcr;

/** Argeo JCR specific exceptions. */
public class ArgeoJcrException extends IllegalStateException {
	private static final long serialVersionUID = -1941940005390084331L;

	public ArgeoJcrException(String message, Throwable e) {
		super(message, e);
	}

	public ArgeoJcrException(String message) {
		super(message);
	}

}
