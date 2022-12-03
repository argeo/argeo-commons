package org.argeo.cms.auth;

/** Transitional interface to decouple from the Servlet API. */
public interface RemoteAuthResponse {
	/** Set this header to a single value, possibly removing previous values. */
	void setHeader(String headerName, String value);

	/** Add a value to this header. */
	void addHeader(String headerName, String value);
}
