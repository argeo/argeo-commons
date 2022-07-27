package org.argeo.util.http;

/**
 * Standard HTTP response status codes.
 * 
 * @see https://developer.mozilla.org/en-US/docs/Web/HTTP/Status
 */
public enum HttpResponseStatus {
	UNAUTHORIZED(401), //
	FORBIDDEN(403), //
	NOT_FOUND(404),//
	;

	private final int statusCode;

	HttpResponseStatus(int statusCode) {
		this.statusCode = statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}

}
