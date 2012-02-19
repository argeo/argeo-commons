package org.argeo.util.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.argeo.ArgeoException;
import org.argeo.StreamUtils;

/** Simple password based encryption / decryption */
public class PasswordBasedEncryption {
	public final static Integer DEFAULT_ITERATION_COUNT = 1024;
	public final static Integer DEFAULT_KEY_LENGTH = 256;
	public final static String DEFAULT_SECRETE_KEY_FACTORY = "PBKDF2WithHmacSHA1";
	public final static String DEFAULT_SECRETE_KEY_ENCRYPTION = "AES";
	public final static String DEFAULT_CIPHER = "AES/CBC/PKCS5Padding";
	public final static String DEFAULT_CHARSET = "UTF-8";

	private static byte[] DEFAULT_SALT_8 = { (byte) 0xA9, (byte) 0x9B,
			(byte) 0xC8, (byte) 0x32, (byte) 0x56, (byte) 0x35, (byte) 0xE3,
			(byte) 0x03 };
	private static byte[] DEFAULT_IV_16 = { (byte) 0xA9, (byte) 0x9B,
			(byte) 0xC8, (byte) 0x32, (byte) 0x56, (byte) 0x35, (byte) 0xE3,
			(byte) 0x03, (byte) 0xA9, (byte) 0x9B, (byte) 0xC8, (byte) 0x32,
			(byte) 0x56, (byte) 0x35, (byte) 0xE3, (byte) 0x03 };

	private final Key key;
	private final Cipher ecipher;
	private final Cipher dcipher;

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
			byte[] salt = new byte[8];
			System.arraycopy(passwordSalt, 0, salt, 0, salt.length);
			// for (int i = 0; i < password.length && i < salt.length; i++)
			// salt[i] = (byte) password[i];
			byte[] iv = new byte[16];
			System.arraycopy(initializationVector, 0, iv, 0, iv.length);
			// for (int i = 0; i < password.length && i < iv.length; i++)
			// iv[i] = (byte) password[i];

			SecretKeyFactory keyFac = SecretKeyFactory
					.getInstance(getSecretKeyFactoryName());
			PBEKeySpec keySpec = new PBEKeySpec(password, salt,
					getIterationCount(), getKeyLength());
			String secKeyEncryption = getSecretKeyEncryption();
			if (secKeyEncryption != null) {
				SecretKey tmp = keyFac.generateSecret(keySpec);
				key = new SecretKeySpec(tmp.getEncoded(),
						getSecretKeyEncryption());
			} else {
				key = keyFac.generateSecret(keySpec);
			}
			ecipher = Cipher.getInstance(getCipherName());
			ecipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
			// AlgorithmParameters params = ecipher.getParameters();
			// byte[] iv =
			// params.getParameterSpec(IvParameterSpec.class).getIV();
			dcipher = Cipher.getInstance(getCipherName());
			dcipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
		} catch (Exception e) {
			throw new ArgeoException("Cannot get secret key", e);
		}
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
			throw new ArgeoException("Cannot encrypt", e);
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
			throw new ArgeoException("Cannot decrypt", e);
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
			throw new ArgeoException("Cannot encrypt", e);
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
			throw new ArgeoException("Cannot decrypt", e);
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
		return DEFAULT_ITERATION_COUNT;
	}

	protected Integer getKeyLength() {
		return DEFAULT_KEY_LENGTH;
	}

	protected String getSecretKeyFactoryName() {
		return DEFAULT_SECRETE_KEY_FACTORY;
	}

	protected String getSecretKeyEncryption() {
		return DEFAULT_SECRETE_KEY_ENCRYPTION;
	}

	protected String getCipherName() {
		return DEFAULT_CIPHER;
	}
}
