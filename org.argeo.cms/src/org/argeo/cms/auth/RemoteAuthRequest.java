package org.argeo.cms.auth;

import java.util.Locale;

/** Transitional interface to decouple from the Servlet API. */
public interface RemoteAuthRequest {
	final static String REMOTE_USER = "org.osgi.service.http.authentication.remote.user";
	final static String AUTHORIZATION = "org.osgi.service.useradmin.authorization";

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
