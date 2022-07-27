package org.argeo.util.http;

/** HTTP headers which are specific to WebDAV. */
public enum HttpHeader {
	AUTHORIZATION("Authorization"), //
	WWW_AUTHENTICATE("WWW-Authenticate"), //
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
