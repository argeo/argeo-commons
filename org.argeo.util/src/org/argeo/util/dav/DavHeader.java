package org.argeo.util.dav;

/** Standard HTTP headers. */
public enum DavHeader {
	DEPTH("Depth"), //
	;

	private final String name;

	private DavHeader(String headerName) {
		this.name = headerName;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return getName();
	}

}
