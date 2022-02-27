package org.argeo.cms.auth;

/** Transitional interface to decouple from the Servlet API. */
public interface RemoteAuthSession {
	boolean isValid();

	String getId();
}
