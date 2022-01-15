package org.argeo.util;

/** Utilities around byte arrays and byte buffers. */
public class BytesUtils {
	final private static char[] hexArray = "0123456789abcdef".toCharArray();

	/** Convert two longs to a byte array with length 16. */
	public static byte[] toBytes(long long1, long long2) {
		byte[] result = new byte[16];
		for (int i = 0; i < 8; i++)
			result[i] = (byte) ((long1 >> ((7 - i) * 8)) & 0xff);
		for (int i = 8; i < 16; i++)
			result[i] = (byte) ((long2 >> ((15 - i) * 8)) & 0xff);
		return result;
	}

	public static void copyBytes(long long1, long long2, byte[] arr, int offset) {
		assert arr.length >= 16 + offset;
		for (int i = offset; i < 8 + offset; i++)
			arr[i] = (byte) ((long1 >> ((7 - i) * 8)) & 0xff);
		for (int i = 8 + offset; i < 16 + offset; i++)
			arr[i] = (byte) ((long2 >> ((15 - i) * 8)) & 0xff);
	}

	/** Converts a byte array to an hex String. */
	public static String toHexString(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/** singleton */
	private BytesUtils() {

	}

}
