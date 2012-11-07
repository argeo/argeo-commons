package org.argeo.osgi.boot;

/** OsgiBoot specific exceptions */
public class OsgiBootException extends RuntimeException {
	private static final long serialVersionUID = 2414011711711425353L;

	public OsgiBootException() {
	}

	public OsgiBootException(String message) {
		super(message);
	}

	public OsgiBootException(String message, Throwable cause) {
		super(message, cause);
	}

}
