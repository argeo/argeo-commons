package org.argeo.cms.auth;

import java.util.function.Supplier;

/** Authentication information to send as a remote response. */
public interface RemoteAuthResponse {
	/** Set this header to a single value, possibly removing previous values. */
	void setHeader(String headerName, String value);

	/** Add a value to this header. */
	void addHeader(String headerName, String value);

	/*
	 * CONVENIENCE METHODS
	 */
	/** Convenience method calling {@link #setHeader(String, String)}. */
	default void setHeader(Supplier<String> headerName, String value) {
		setHeader(headerName.get(), value);
	}

	/** Convenience method calling {@link #addHeader(String, String)}. */
	default void addHeader(Supplier<String> headerName, String value) {
		addHeader(headerName.get(), value);
	}

}
