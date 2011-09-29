package org.argeo.util.crypto;

import java.io.InputStream;

/**
 * Access to private (typically encrypted) data. The keyring is responsible for
 * retrieving the necessary credentials.
 */
public interface Keyring {
	public void changePassword(char[] oldPassword, char[] newPassword);

	public char[] getAsChars(String path);

	public InputStream getAsStream(String path);

	public void set(String path, char[] arr);

	public void set(String path, InputStream in);
}
