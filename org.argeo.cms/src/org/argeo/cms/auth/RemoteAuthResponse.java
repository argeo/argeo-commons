package org.argeo.cms.auth;

/** Transitional interface to decouple from the Servlet API. */
public interface RemoteAuthResponse {
	void setHeader(String keys, String value);

}
