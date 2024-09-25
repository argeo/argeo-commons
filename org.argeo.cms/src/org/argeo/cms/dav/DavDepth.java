package org.argeo.cms.dav;

import static org.argeo.cms.http.HttpHeader.DEPTH;

import java.util.function.Supplier;

import com.sun.net.httpserver.HttpExchange;

/** WebDav depth possible values. */
public enum DavDepth implements Supplier<String> {
	DEPTH_0("0"), //
	DEPTH_1("1"), //
	DEPTH_INFINITY("infinity");

	private final String value;

	private DavDepth(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return get();
	}

	/** @deprecated Use {@link #get()} instead. */
	@Deprecated
	public String getValue() {
		return get();
	}

	@Override
	public String get() {
		return value;
	}

	public static DavDepth fromHttpExchange(HttpExchange httpExchange) {
		String value = httpExchange.getRequestHeaders().getFirst(DEPTH.get());
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
