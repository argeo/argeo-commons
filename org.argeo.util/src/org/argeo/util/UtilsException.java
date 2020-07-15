package org.argeo.util;

/** Utils specific exception. */
class UtilsException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	/** Creates an exception with a message. */
	public UtilsException(String message) {
		super(message);
	}

	/** Creates an exception with a message and a root cause. */
	public UtilsException(String message, Throwable e) {
		super(message, e);
	}

}
