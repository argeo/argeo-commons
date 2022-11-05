package org.argeo.cms.http;

/** Standard HTTP headers (including WebDav). */
public enum HttpHeader {
	AUTHORIZATION("Authorization"), //
	WWW_AUTHENTICATE("WWW-Authenticate"), //
	ALLOW("Allow"), //

	// WebDav
	DAV("DAV"), //
	DEPTH("Depth"), //
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
