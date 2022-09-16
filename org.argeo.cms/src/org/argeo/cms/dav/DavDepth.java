package org.argeo.cms.dav;

import org.argeo.util.http.HttpHeader;

import com.sun.net.httpserver.HttpExchange;

public enum DavDepth {
	DEPTH_0("0"), DEPTH_1("1"), DEPTH_INFINITY("infinity");

	private final String value;

	private DavDepth(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return getValue();
	}

	public String getValue() {
		return value;
	}

	public static DavDepth fromHttpExchange(HttpExchange httpExchange) {
		String value = httpExchange.getRequestHeaders().getFirst(HttpHeader.DEPTH.getHeaderName());
		if (value == null)
			return null;
		DavDepth depth = switch (value) {
		case "0" -> DEPTH_0;
		case "1" -> DEPTH_1;
		case "infinity" -> DEPTH_INFINITY;
		default -> throw new IllegalArgumentException("Unexpected value: " + value);
		};
		return depth;
	}
}
