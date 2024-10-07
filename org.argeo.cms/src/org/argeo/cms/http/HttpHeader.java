package org.argeo.cms.http;

import java.util.function.Supplier;

/** Selection of standard or common HTTP headers (including WebDav). */
public enum HttpHeader implements Supplier<String> {
	AUTHORIZATION("Authorization"), //
	WWW_AUTHENTICATE("WWW-Authenticate"), //
	ALLOW("Allow"), //
	VIA("Via"), //
	/**
	 * For example:
	 * 
	 * <pre>
	 * Content-Type: text/html; charset=utf-8
	 * Content-Type: multipart/form-data; boundary=ExampleBoundaryString
	 * </pre>
	 * 
	 * @see <a href=
	 *      "https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type">MDN
	 *      Web Docs</a>
	 */
	CONTENT_TYPE("Content-Type"), //
	CONTENT_LENGTH("Content-Length"), //
	CONTENT_DISPOSITION("Content-Disposition"), //
	DATE("Date"), //

	// WebDav
	DAV("DAV"), //
	DEPTH("Depth"), //

	// Non-standard
	X_FORWARDED_HOST("X-Forwarded-Host"), //
	;

	// WWW-Authenticate related constants
	public final static String BASIC = "Basic";
	public final static String REALM = "realm";
	public final static String NEGOTIATE = "Negotiate";

	// Content-Disposition related constants
	public final static String ATTACHMENT = "attachment";
	public final static String FILENAME = "filename";

	// Content-Type related constants
	public final static String CHARSET = "charset";
	public final static String BOUNDARY = "charset";

	private final String headerName;

	private HttpHeader(String headerName) {
		this.headerName = headerName;
	}

	/** @deprecated Use {@link #get()} instead. */
	@Deprecated
	public String getHeaderName() {
		return get();
	}

	@Override
	public String get() {
		return headerName;
	}

	@Override
	public String toString() {
		return get();
	}

	/*
	 * STATIC UTILITIES
	 * 
	 * Canonical utilities, typically when a header has parameters or a specific
	 * format.
	 */
	/**
	 * Format a Content-Type header with parameters.
	 * 
	 * @see <a href=
	 *      "https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Type#directives">MDN
	 *      Web Docs</a>
	 */
	public static String formatContentType(String mediaType, String charset, String boundary) {
		// not clear whether both encoding and boundary can be set; we won't enforce
		return mediaType //
				+ (charset != null ? ";" + CHARSET + "=" + charset.toLowerCase() : "") //
				+ (boundary != null ? ";" + BOUNDARY + "=" + boundary : "") //
		;
	}
}
