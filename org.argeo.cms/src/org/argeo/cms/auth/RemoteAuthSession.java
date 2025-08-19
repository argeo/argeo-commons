package org.argeo.cms.auth;

/**
 * A minimal facade to a persistent session during a remote exchange, to be used
 * solely for authentication purposes.
 */
public interface RemoteAuthSession {
	/** Whether this session is valid. */
	boolean isValid();

	/** A locally unique id identifying this session. */
	// TODO rather use an Object?
	String getId();
}
