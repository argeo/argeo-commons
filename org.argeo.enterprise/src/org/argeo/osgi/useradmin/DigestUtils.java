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

	private DigestUtils() {
	}

}
