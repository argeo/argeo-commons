package org.argeo.cms.auth;

import java.util.Locale;

/** Transitional interface to decouple from the Servlet API. */
public interface RemoteAuthRequest {
	RemoteAuthSession getSession();

	RemoteAuthSession createSession();

	Locale getLocale();

	Object getAttribute(String key);

	void setAttribute(String key, Object object);

	String getHeader(String key);

	String getRemoteAddr();

	int getLocalPort();

	int getRemotePort();

}
