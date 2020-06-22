package org.argeo.osgi.useradmin;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.spec.KeySpec;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Utilities around digests, mostly those related to passwords. */
class DigestUtils {
	final static String PASSWORD_SCHEME_SHA = "SHA";
	final static String PASSWORD_SCHEME_PBKDF2_SHA256 = "PBKDF2_SHA256";

	static byte[] sha1(byte[] bytes) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA1");
			digest.update(bytes);
			byte[] checksum = digest.digest();
			return checksum;
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot SHA1 digest", e);
		}
	}

	static byte[] toPasswordScheme(String passwordScheme, char[] password, byte[] salt, Integer iterations,
			Integer keyLength) {
		try {
			if (PASSWORD_SCHEME_SHA.equals(passwordScheme)) {
				MessageDigest digest = MessageDigest.getInstance("SHA1");
				byte[] bytes = charsToBytes(password);
				digest.update(bytes);
				return digest.digest();
			} else if (PASSWORD_SCHEME_PBKDF2_SHA256.equals(passwordScheme)) {
				KeySpec spec = new PBEKeySpec(password, salt, iterations, keyLength);

				SecretKeyFactory f = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
				final int ITERATION_LENGTH = 4;
				byte[] key = f.generateSecret(spec).getEncoded();
				byte[] result = new byte[ITERATION_LENGTH + salt.length + key.length];
				byte iterationsArr[] = new BigInteger(iterations.toString()).toByteArray();
				if (iterationsArr.length < ITERATION_LENGTH) {
					Arrays.fill(result, 0, ITERATION_LENGTH - iterationsArr.length, (byte) 0);
					System.arraycopy(iterationsArr, 0, result, ITERATION_LENGTH - iterationsArr.length,
							iterationsArr.length);
				} else {
					System.arraycopy(iterationsArr, 0, result, 0, ITERATION_LENGTH);
				}
				System.arraycopy(salt, 0, result, ITERATION_LENGTH, salt.length);
				System.arraycopy(key, 0, result, ITERATION_LENGTH + salt.length, key.length);
				return result;
			} else {
				throw new UnsupportedOperationException("Unkown password scheme " + passwordScheme);
			}
		} catch (Exception e) {
			throw new UserDirectoryException("Cannot digest", e);
		}
	}

	static char[] bytesToChars(Object obj) {
		if (obj instanceof char[])
			return (char[]) obj;
		if (!(obj instanceof byte[]))
			throw new IllegalArgumentException(obj.getClass() + " is not a byte array");
		ByteBuffer fromBuffer = ByteBuffer.wrap((byte[]) obj);
		CharBuffer toBuffer = StandardCharsets.UTF_8.decode(fromBuffer);
		char[] res = Arrays.copyOfRange(toBuffer.array(), toBuffer.position(), toBuffer.limit());
		// Arrays.fill(fromBuffer.array(), (byte) 0); // clear sensitive data
		// Arrays.fill((byte[]) obj, (byte) 0); // clear sensitive data
		// Arrays.fill(toBuffer.array(), '\u0000'); // clear sensitive data
		return res;
	}

	static byte[] charsToBytes(char[] chars) {
		CharBuffer charBuffer = CharBuffer.wrap(chars);
		ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode(charBuffer);
		byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
		// Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
		// Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return bytes;
	}

	static String sha1str(String str) {
		byte[] hash = sha1(str.getBytes(StandardCharsets.UTF_8));
		return encodeHexString(hash);
	}

	final private static char[] hexArray = "0123456789abcdef".toCharArray();

	/**
	 * From
	 * http://stackoverflow.com/questions/9655181/how-to-convert-a-byte-array-to
	 * -a-hex-string-in-java
	 */
	public static String encodeHexString(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	private DigestUtils() {
	}
}
