package org.argeo.util.http;

/**
 * Standard HTTP response status codes.
 * 
 * @see https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
 */
public enum HttpResponseStatus {
	// Successful responses (200–299)
	OK(200), //
	MULTI_STATUS(207), // WebDav
	// Client error responses (400–499)
	UNAUTHORIZED(401), //
	FORBIDDEN(403), //
	NOT_FOUND(404), //
	;

	private final int code;

	HttpResponseStatus(int statusCode) {
		this.code = statusCode;
	}

	public int getCode() {
		return code;
	}

}
