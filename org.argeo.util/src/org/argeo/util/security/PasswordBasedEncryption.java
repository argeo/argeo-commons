/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.util.security;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.argeo.util.internal.UtilsException;
import org.argeo.util.internal.StreamUtils;

/** Simple password based encryption / decryption */
public class PasswordBasedEncryption {
	public final static Integer DEFAULT_ITERATION_COUNT = 1024;
	/** Stronger with 256, but causes problem with Oracle JVM */
	public final static Integer DEFAULT_SECRETE_KEY_LENGTH = 256;
	public final static Integer DEFAULT_SECRETE_KEY_LENGTH_RESTRICTED = 128;
	public final static String DEFAULT_SECRETE_KEY_FACTORY = "PBKDF2WithHmacSHA1";
	public final static String DEFAULT_SECRETE_KEY_ENCRYPTION = "AES";
	public final static String DEFAULT_CIPHER_NAME = "AES/CBC/PKCS5Padding";
	public final static String DEFAULT_CHARSET = "UTF-8";

	private Integer iterationCount = DEFAULT_ITERATION_COUNT;
	private Integer secreteKeyLength = DEFAULT_SECRETE_KEY_LENGTH;
	private String secreteKeyFactoryName = DEFAULT_SECRETE_KEY_FACTORY;
	private String secreteKeyEncryption = DEFAULT_SECRETE_KEY_ENCRYPTION;
	private String cipherName = DEFAULT_CIPHER_NAME;

	private static byte[] DEFAULT_SALT_8 = { (byte) 0xA9, (byte) 0x9B,
			(byte) 0xC8, (byte) 0x32, (byte) 0x56, (byte) 0x35, (byte) 0xE3,
			(byte) 0x03 };
	private static byte[] DEFAULT_IV_16 = { (byte) 0xA9, (byte) 0x9B,
			(byte) 0xC8, (byte) 0x32, (byte) 0x56, (byte) 0x35, (byte) 0xE3,
			(byte) 0x03, (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
			(byte) 0x56, (byte) 0x35, (byte) 0xE3, (byte) 0x03 };

	private Key key;
	private Cipher ecipher;
	private Cipher dcipher;

	private String securityProviderName = null;

	/**
	 * This is up to the caller to clear the passed array. Neither copy of nor
	 * reference to the passed array is kept
	 */
	public PasswordBasedEncryption(char[] password) {
		this(password, DEFAULT_SALT_8, DEFAULT_IV_16);
	}

	/**
	 * This is up to the caller to clear the passed array. Neither copies of nor
	 * references to the passed arrays are kept
	 */
	public PasswordBasedEncryption(char[] password, byte[] passwordSalt,
			byte[] initializationVector) {
		try {
			initKeyAndCiphers(password, passwordSalt, initializationVector);
		} catch (InvalidKeyException e) {
			Integer previousSecreteKeyLength = secreteKeyLength;
			secreteKeyLength = DEFAULT_SECRETE_KEY_LENGTH_RESTRICTED;
			System.err.println("'" + e.getMessage() + "', will use "
					+ secreteKeyLength + " secrete key length instead of "
					+ previousSecreteKeyLength);
			try {
				initKeyAndCiphers(password, passwordSalt, initializationVector);
			} catch (Exception e1) {
				throw new UtilsException(
						"Cannot get secret key (with restricted length)", e1);
			}
		} catch (Exception e) {
			throw new UtilsException("Cannot get secret key", e);
		}
	}

	protected void initKeyAndCiphers(char[] password, byte[] passwordSalt,
			byte[] initializationVector) throws GeneralSecurityException {
		byte[] salt = new byte[8];
		System.arraycopy(passwordSalt, 0, salt, 0, salt.length);
		// for (int i = 0; i < password.length && i < salt.length; i++)
		// salt[i] = (byte) password[i];
		byte[] iv = new byte[16];
		System.arraycopy(initializationVector, 0, iv, 0, iv.length);

		SecretKeyFactory keyFac = SecretKeyFactory
				.getInstance(getSecretKeyFactoryName());
		PBEKeySpec keySpec = new PBEKeySpec(password, salt,
				getIterationCount(), getKeyLength());
		String secKeyEncryption = getSecretKeyEncryption();
		if (secKeyEncryption != null) {
			SecretKey tmp = keyFac.generateSecret(keySpec);
			key = new SecretKeySpec(tmp.getEncoded(), getSecretKeyEncryption());
		} else {
			key = keyFac.generateSecret(keySpec);
		}
		if (securityProviderName != null)
			ecipher = Cipher.getInstance(getCipherName(), securityProviderName);
		else
			ecipher = Cipher.getInstance(getCipherName());
		ecipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
		dcipher = Cipher.getInstance(getCipherName());
		dcipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
	}

	public void encrypt(InputStream decryptedIn, OutputStream encryptedOut)
			throws IOException {
		try {
			CipherOutputStream out = new CipherOutputStream(encryptedOut,
					ecipher);
			StreamUtils.copy(decryptedIn, out);
			StreamUtils.closeQuietly(out);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new UtilsException("Cannot encrypt", e);
		} finally {
			StreamUtils.closeQuietly(decryptedIn);
		}
	}

	public void decrypt(InputStream encryptedIn, OutputStream decryptedOut)
			throws IOException {
		try {
			CipherInputStream decryptedIn = new CipherInputStream(encryptedIn,
					dcipher);
			StreamUtils.copy(decryptedIn, decryptedOut);
		} catch (IOException e) {
			throw e;
		} catch (Exception e) {
			throw new UtilsException("Cannot decrypt", e);
		} finally {
			StreamUtils.closeQuietly(encryptedIn);
		}
	}

	public byte[] encryptString(String str) {
		ByteArrayOutputStream out = null;
		ByteArrayInputStream in = null;
		try {
			out = new ByteArrayOutputStream();
			in = new ByteArrayInputStream(str.getBytes(DEFAULT_CHARSET));
			encrypt(in, out);
			return out.toByteArray();
		} catch (Exception e) {
			throw new UtilsException("Cannot encrypt", e);
		} finally {
			StreamUtils.closeQuietly(out);
		}
	}

	/** Closes the input stream */
	public String decryptAsString(InputStream in) {
		ByteArrayOutputStream out = null;
		try {
			out = new ByteArrayOutputStream();
			decrypt(in, out);
			return new String(out.toByteArray(), DEFAULT_CHARSET);
		} catch (Exception e) {
			throw new UtilsException("Cannot decrypt", e);
		} finally {
			StreamUtils.closeQuietly(out);
		}
	}

	protected Key getKey() {
		return key;
	}

	protected Cipher getEcipher() {
		return ecipher;
	}

	protected Cipher getDcipher() {
		return dcipher;
	}

	protected Integer getIterationCount() {
		return iterationCount;
	}

	protected Integer getKeyLength() {
		return secreteKeyLength;
	}

	protected String getSecretKeyFactoryName() {
		return secreteKeyFactoryName;
	}

	protected String getSecretKeyEncryption() {
		return secreteKeyEncryption;
	}

	protected String getCipherName() {
		return cipherName;
	}

	public void setIterationCount(Integer iterationCount) {
		this.iterationCount = iterationCount;
	}

	public void setSecreteKeyLength(Integer keyLength) {
		this.secreteKeyLength = keyLength;
	}

	public void setSecreteKeyFactoryName(String secreteKeyFactoryName) {
		this.secreteKeyFactoryName = secreteKeyFactoryName;
	}

	public void setSecreteKeyEncryption(String secreteKeyEncryption) {
		this.secreteKeyEncryption = secreteKeyEncryption;
	}

	public void setCipherName(String cipherName) {
		this.cipherName = cipherName;
	}

	public void setSecurityProviderName(String securityProviderName) {
		this.securityProviderName = securityProviderName;
	}
}
