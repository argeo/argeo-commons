package org.argeo.cms.auth;

import java.util.Locale;
import java.util.function.Supplier;

/** Authentication information received from a remote request. */
public interface RemoteAuthRequest {
	final static String REMOTE_USER = "org.osgi.service.http.authentication.remote.user";
	final static String AUTHORIZATION = "org.osgi.service.useradmin.authorization";

	/** Can be null. */
	RemoteAuthSession getSession();

	/** May not be implemented, will return null in that case. */
	RemoteAuthSession createSession();

	Locale getLocale();

	Object getAttribute(String key);

	void setAttribute(String key, Object object);

	/** The (single) value of a header. */
	String getHeader(String headerName);

	String getRemoteAddr();

	int getLocalPort();

	int getRemotePort();

	/*
	 * CONVENIENCE METHODS
	 */
	/** Convenience method calling {@link #getHeader(String)}. */
	default String getHeader(Supplier<String> headerName) {
		return getHeader(headerName.get());
	}
}
