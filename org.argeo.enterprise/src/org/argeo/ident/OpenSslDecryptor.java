package org.argeo.ident;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Decrypts OpenSSL encrypted data.
 * 
 * From
 * https://stackoverflow.com/questions/11783062/how-to-decrypt-file-in-java-encrypted-with-openssl-command-using-aes
 * 
 * See also
 * https://stackoverflow.com/questions/54171959/badpadding-exception-when-trying-to-decrypt-aes-based-encrypted-text/54173509#54173509
 * for new default message digest (not yet in CentOS 7 as of July 2019)
 */
public class OpenSslDecryptor {
	private static final int INDEX_KEY = 0;
	private static final int INDEX_IV = 1;
	private static final int ITERATIONS = 1;

	private static final int SALT_OFFSET = 8;
	private static final int SALT_SIZE = 8;
	private static final int CIPHERTEXT_OFFSET = SALT_OFFSET + SALT_SIZE;

	/** In bits. */
	private final int keySize;

	private Cipher cipher;
	private MessageDigest messageDigest;

	public OpenSslDecryptor() {
		/*
		 * Changed to SHA-256 from OpenSSL v1.1.0 (see
		 * https://stackoverflow.com/questions/39637388/encryption-decryption-doesnt-
		 * work-well-between-two-different-openssl-versions)
		 */
		this(128, "MD5");
	}

	public OpenSslDecryptor(int keySize, String messageDigest) {
		this.keySize = keySize;
		try {
			this.cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			this.messageDigest = MessageDigest.getInstance(messageDigest);
		} catch (GeneralSecurityException e) {
			throw new IllegalArgumentException("Cannot initialise decryptor", e);
		}
	}

	public String decryptAuthd(String dataBase64, String passphrase) {
		try {
			byte[] headerSaltAndCipherText = Base64.getDecoder().decode(dataBase64);

			boolean withSalt = true;
			byte[] salt = withSalt ? Arrays.copyOfRange(headerSaltAndCipherText, SALT_OFFSET, SALT_OFFSET + SALT_SIZE)
					: null;
			byte[] encrypted = withSalt
					? Arrays.copyOfRange(headerSaltAndCipherText, CIPHERTEXT_OFFSET, headerSaltAndCipherText.length)
					: headerSaltAndCipherText;

			final byte[][] keyAndIV = EVP_BytesToKey(keySize / Byte.SIZE, cipher.getBlockSize(), messageDigest, salt,
					passphrase.getBytes(StandardCharsets.US_ASCII), ITERATIONS);
			SecretKeySpec key = new SecretKeySpec(keyAndIV[INDEX_KEY], "AES");
			IvParameterSpec iv = new IvParameterSpec(keyAndIV[INDEX_IV]);

			cipher.init(Cipher.DECRYPT_MODE, key, iv);
			byte[] decrypted = cipher.doFinal(encrypted);

			String answer = new String(decrypted, StandardCharsets.US_ASCII);
			return answer;
		} catch (BadPaddingException e) {
			throw new IllegalStateException("Bad password, algorithm, mode or padding;"
					+ " no salt, wrong number of iterations or corrupted ciphertext.", e);
		} catch (IllegalBlockSizeException e) {
			throw new IllegalStateException("Bad algorithm, mode or corrupted (resized) ciphertext.", e);
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException(e);
		}
	}

	private static byte[][] EVP_BytesToKey(int key_len, int iv_len, MessageDigest md, byte[] salt, byte[] data,
			int count) {
		byte[][] both = new byte[2][];
		byte[] key = new byte[key_len];
		int key_ix = 0;
		byte[] iv = new byte[iv_len];
		int iv_ix = 0;
		both[0] = key;
		both[1] = iv;
		byte[] md_buf = null;
		int nkey = key_len;
		int niv = iv_len;
		int i = 0;
		if (data == null) {
			return both;
		}
		int addmd = 0;
		for (;;) {
			md.reset();
			if (addmd++ > 0) {
				md.update(md_buf);
			}
			md.update(data);
			if (null != salt) {
				md.update(salt, 0, 8);
			}
			md_buf = md.digest();
			for (i = 1; i < count; i++) {
				md.reset();
				md.update(md_buf);
				md_buf = md.digest();
			}
			i = 0;
			if (nkey > 0) {
				for (;;) {
					if (nkey == 0)
						break;
					if (i == md_buf.length)
						break;
					key[key_ix++] = md_buf[i];
					nkey--;
					i++;
				}
			}
			if (niv > 0 && i != md_buf.length) {
				for (;;) {
					if (niv == 0)
						break;
					if (i == md_buf.length)
						break;
					iv[iv_ix++] = md_buf[i];
					niv--;
					i++;
				}
			}
			if (nkey == 0 && niv == 0) {
				break;
			}
		}
		for (i = 0; i < md_buf.length; i++) {
			md_buf[i] = 0;
		}
		return both;
	}

	public static void main(String[] args) {
		String dataBase64 = args[0];
		String passphrase = args[1];
		OpenSslDecryptor decryptor = new OpenSslDecryptor();
		System.out.println(decryptor.decryptAuthd(dataBase64, passphrase));
	}

}