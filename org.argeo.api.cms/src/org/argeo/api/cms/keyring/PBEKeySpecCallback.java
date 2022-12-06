package org.argeo.api.cms.keyring;

import javax.crypto.spec.PBEKeySpec;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;

/**
 * All information required to set up a {@link PBEKeySpec} bar the password
 * itself (use a {@link PasswordCallback})
 */
public class PBEKeySpecCallback implements Callback {
	private String secretKeyFactory;
	private byte[] salt;
	private Integer iterationCount;
	/** Can be null for some algorithms */
	private Integer keyLength;
	/** Can be null, will trigger secret key encryption if not */
	private String secretKeyEncryption;

	private String encryptedPasswordHashCipher;
	private byte[] encryptedPasswordHash;

	public void set(String secretKeyFactory, byte[] salt,
			Integer iterationCount, Integer keyLength,
			String secretKeyEncryption) {
		this.secretKeyFactory = secretKeyFactory;
		this.salt = salt;
		this.iterationCount = iterationCount;
		this.keyLength = keyLength;
		this.secretKeyEncryption = secretKeyEncryption;
//		this.encryptedPasswordHashCipher = encryptedPasswordHashCipher;
//		this.encryptedPasswordHash = encryptedPasswordHash;
	}

	public String getSecretKeyFactory() {
		return secretKeyFactory;
	}

	public byte[] getSalt() {
		return salt;
	}

	public Integer getIterationCount() {
		return iterationCount;
	}

	public Integer getKeyLength() {
		return keyLength;
	}

	public String getSecretKeyEncryption() {
		return secretKeyEncryption;
	}

	public String getEncryptedPasswordHashCipher() {
		return encryptedPasswordHashCipher;
	}

	public byte[] getEncryptedPasswordHash() {
		return encryptedPasswordHash;
	}

}
