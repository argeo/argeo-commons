package org.argeo.api.security;

import java.io.InputStream;

/**
 * Access to private (typically encrypted) data. The keyring is responsible for
 * retrieving the necessary credentials. <b>Experimental. This API may
 * change.</b>
 */
public interface Keyring {
	/**
	 * Returns the confidential information as chars. Must ask for it if it is
	 * not stored.
	 */
	public char[] getAsChars(String path);

	/**
	 * Returns the confidential information as a stream. Must ask for it if it
	 * is not stored.
	 */
	public InputStream getAsStream(String path);

	public void set(String path, char[] arr);

	public void set(String path, InputStream in);
}
