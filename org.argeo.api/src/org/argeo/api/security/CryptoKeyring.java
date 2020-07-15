package org.argeo.api.security;

/**
 * Marker interface for an advanced keyring based on cryptography.
 */
public interface CryptoKeyring extends Keyring {
	public void changePassword(char[] oldPassword, char[] newPassword);

	public void unlock(char[] password);
}
