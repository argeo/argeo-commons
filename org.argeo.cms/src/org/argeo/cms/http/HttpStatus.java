package org.argeo.cms.http;

import java.util.function.Supplier;

/**
 * Standard HTTP response status codes (including WebDav ones).
 * 
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Status">MDN
 *      Web Docs</a>
 */
public enum HttpStatus implements Supplier<Integer> {
	// Successful responses (200–299)
	/** 200 */
	OK(200, "OK"), //
	/** 204 */
	NO_CONTENT(204, "No Content"), //
	/** 207 */
	MULTI_STATUS(207, "Multi-Status"), // WebDav
	// Client error responses (400–499)
	/** 401 */
	UNAUTHORIZED(401, "Unauthorized"), //
	/** 403 */
	FORBIDDEN(403, "Forbidden"), //
	/** 404 */
	NOT_FOUND(404, "Not Found"), //
	// Server error responses (500-599)
	/** 500 */
	INTERNAL_SERVER_ERROR(500, "Internal Server Error"), //
	/** 501 */
	NOT_IMPLEMENTED(501, "Not Implemented"), //
	;

	private final int code;
	private final String reasonPhrase;

	HttpStatus(int statusCode, String reasonPhrase) {
		this.code = statusCode;
		this.reasonPhrase = reasonPhrase;
	}

	/** @deprecated Use {@link #get()} instead. */
	@Deprecated
	public int getCode() {
		return code;
	}

	@Override
	public Integer get() {
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
				if (status.get() == code)
					return status;
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Invalid status line: " + statusLine, e);
		}
		throw new IllegalArgumentException("Unkown status code: " + statusLine);
	}

	@Override
	public String toString() {
		return get() + " " + getReasonPhrase();
	}

}
