package org.argeo.cms.http;

/** Selection of standard or common HTTP headers (including WebDav). */
public enum HttpHeader {
	AUTHORIZATION("Authorization"), //
	WWW_AUTHENTICATE("WWW-Authenticate"), //
	ALLOW("Allow"), //
	VIA("Via"), //
	CONTENT_TYPE("Content-Type"), //

	// WebDav
	DAV("DAV"), //
	DEPTH("Depth"), //

	// Non-standard
	X_FORWARDED_HOST("X-Forwarded-Host"), //
	;

	public final static String BASIC = "Basic";
	public final static String REALM = "realm";
	public final static String NEGOTIATE = "Negotiate";

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
