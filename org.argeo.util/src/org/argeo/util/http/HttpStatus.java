package org.argeo.util.http;

/**
 * Standard HTTP response status codes (including WebDav ones).
 * 
 * @see "https://developer.mozilla.org/en-US/docs/Web/HTTP/Status"
 */
public enum HttpStatus {
	// Successful responses (200–299)
	OK(200, "OK"), //
	NO_CONTENT(204, "No Content"), //
	MULTI_STATUS(207, "Multi-Status"), // WebDav
	// Client error responses (400–499)
	UNAUTHORIZED(401, "Unauthorized"), //
	FORBIDDEN(403, "Forbidden"), //
	NOT_FOUND(404, "Not Found"), //
	// Server error responses (500-599)
	INTERNAL_SERVER_ERROR(500, "Internal Server Error"), //
	NOT_IMPLEMENTED(501, "Not Implemented"), //
	;

	private final int code;
	private final String reasonPhrase;

	HttpStatus(int statusCode, String reasonPhrase) {
		this.code = statusCode;
		this.reasonPhrase = reasonPhrase;
	}

	public int getCode() {
		return code;
	}

	public String getReasonPhrase() {
		return reasonPhrase;
	}

	/**
	 * The status line, as defined by RFC2616.
	 * 
	 * @see "https://www.rfc-editor.org/rfc/rfc2616#section-6.1"
	 */
	public String getStatusLine(String httpVersion) {
		return httpVersion + " " + code + " " + reasonPhrase;
	}

	public static HttpStatus parseStatusLine(String statusLine) {
		try {
			String[] arr = statusLine.split(" ");
			int code = Integer.parseInt(arr[1]);
			for (HttpStatus status : values()) {
				if (status.getCode() == code)
					return status;
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid status line: " + statusLine, e);
		}
		throw new IllegalArgumentException("Unkown status code: " + statusLine);
	}

	@Override
	public String toString() {
		return code + " " + reasonPhrase;
	}

}
