package org.argeo.osgi.useradmin;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

/** Utilities around digests, mostly those related to passwords. */
class DigestUtils {
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
