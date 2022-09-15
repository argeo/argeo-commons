package org.argeo.cms.dav;

/** Standard HTTP headers. */
public enum DavHeader {
	DEPTH("Depth"), //
	;

	private final String name;

	private DavHeader(String headerName) {
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
