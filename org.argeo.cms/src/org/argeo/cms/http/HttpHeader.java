package org.argeo.cms.http;

/** Selection of standard or common HTTP headers (including WebDav). */
public enum HttpHeader {
	AUTHORIZATION("Authorization"), //
	WWW_AUTHENTICATE("WWW-Authenticate"), //
	ALLOW("Allow"), //
	VIA("Via"), //
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

	private final String name;

	private HttpHeader(String headerName) {
		this.name = headerName;
	}

	public String getHeaderName() {
		return name;
	}

	@Override
	public String toString() {
		return getHeaderName();
	}

}
