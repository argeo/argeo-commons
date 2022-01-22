package org.argeo.api.acr;

/** When a countent was requested which does not exists, equivalent to HTTP code 404.*/
public class ContentNotFoundException extends RuntimeException {
	private static final long serialVersionUID = -8629074900713760886L;

	public ContentNotFoundException(String message, Throwable cause) {
		super(message, cause);
	}

	public ContentNotFoundException(String message) {
		super(message);
	}

	
}
