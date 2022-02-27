package org.argeo.api.acr;

/**
 * When there is a problem the underlying resources, typically IO, network, DB
 * access, etc.
 */
public class ContentResourceException extends IllegalStateException {
	private static final long serialVersionUID = -2850145213683756996L;

	public ContentResourceException() {
	}

	public ContentResourceException(String s) {
		super(s);
	}

	public ContentResourceException(Throwable cause) {
		super(cause);
	}

	public ContentResourceException(String message, Throwable cause) {
		super(message, cause);
	}

}
