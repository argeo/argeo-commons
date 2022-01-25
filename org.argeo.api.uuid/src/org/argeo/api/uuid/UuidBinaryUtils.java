package org.argeo.api.uuid;

import java.util.Objects;
import java.util.UUID;

/** Static utilities around conversion of {@link UUID} from/to bytes. */
public class UuidBinaryUtils {
	/**
	 * Singleton constructor, should only be extended to provide additional static
	 * utilities.
	 */
	protected UuidBinaryUtils() {
	}

	/**
	 * Convert bytes to an UUID, starting to read the array at this offset.
	 */
	public static UUID fromBytes(byte[] data, int offset) {
		Objects.requireNonNull(data, "Byte array cannot be null");
		long msb = 0;
		long lsb = 0;
		for (int i = offset; i < 8 + offset; i++)
			msb = (msb << 8) | (data[i] & 0xff);
		for (int i = 8 + offset; i < 16 + offset; i++)
			lsb = (lsb << 8) | (data[i] & 0xff);
		return new UUID(msb, lsb);
	}

	/*
	 * UTILITIES
	 */
	/**
	 * Convert bytes to an UUID. Byte array must not be null and be exactly of
	 * length 16.
	 */
	public static UUID fromBytes(byte[] data) {
		Objects.requireNonNull(data, "Byte array must not be null");
		if (data.length != 16)
			throw new IllegalArgumentException("Byte array as length " + data.length);
		return fromBytes(data, 0);
	}

	@Deprecated
	protected static long longFromBytes(byte[] data) {
		long msb = 0;
		for (int i = 0; i < data.length; i++)
			msb = (msb << 8) | (data[i] & 0xff);
		return msb;
	}

	/** Convert this UUID to a byte array of length 16. */
	public static byte[] toBytes(UUID uuid) {
		Objects.requireNonNull(uuid, "UUID cannot be null");
		long msb = uuid.getMostSignificantBits();
		long lsb = uuid.getLeastSignificantBits();
		return toBytes(msb, lsb);
	}

	/** Copies this {@link UUID} as bytes, using 16 bytes. */
	public static void copyBytes(UUID uuid, byte[] arr, int offset) {
		Objects.requireNonNull(uuid, "UUID cannot be null");
		long msb = uuid.getMostSignificantBits();
		long lsb = uuid.getLeastSignificantBits();
		copyBytes(msb, lsb, arr, offset);
	}

	/** Convert two longs to a byte array with length 16. */
	protected static byte[] toBytes(long long1, long long2) {
		byte[] result = new byte[16];
		for (int i = 0; i < 8; i++)
			result[i] = (byte) ((long1 >> ((7 - i) * 8)) & 0xff);
		for (int i = 8; i < 16; i++)
			result[i] = (byte) ((long2 >> ((15 - i) * 8)) & 0xff);
		return result;
	}

	/** Copy these two longs to this byte array, using 16 bytes. */
	protected static void copyBytes(long long1, long long2, byte[] arr, int offset) {
		assert arr.length >= 16 + offset;
		for (int i = offset; i < 8 + offset; i++)
			arr[i] = (byte) ((long1 >> ((7 - i) * 8)) & 0xff);
		for (int i = 8 + offset; i < 16 + offset; i++)
			arr[i] = (byte) ((long2 >> ((15 - i) * 8)) & 0xff);
	}

	final protected static char[] hexArray = "0123456789abcdef".toCharArray();

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

	/*
	 * COMPACT STRING
	 */

	/** To a 32 characters hex string without '-'. */
	public static String toCompact(UUID uuid) {
		return toHexString(toBytes(uuid));
	}

	/**
	 * Converts an UUID hex representation without '-' to an {@link UUID}.
	 */
	public static UUID fromCompact(String compact) {
		return UUID.fromString(UuidBinaryUtils.compactToStd(compact));
	}

	/**
	 * Converts an UUID hex representation without '-' to the standard form (with
	 * '-').
	 */
	public static String compactToStd(String compact) {
		if (compact.length() != 32)
			throw new IllegalArgumentException(
					"Compact UUID '" + compact + "' has length " + compact.length() + " and not 32.");
		StringBuilder sb = new StringBuilder(36);
		for (int i = 0; i < 32; i++) {
			if (i == 8 || i == 12 || i == 16 || i == 20)
				sb.append('-');
			sb.append(compact.charAt(i));
		}
		String std = sb.toString();
		assert std.length() == 36;
		assert UUID.fromString(std).toString().equals(std);
		return std;
	}

	/**
	 * Converts an UUID to a binary string (list of 0 and 1), with a separator to
	 * make it more readable.
	 */
	public static String toBinaryString(UUID uuid, int charsPerSegment, char separator) {
		Objects.requireNonNull(uuid, "UUID cannot be null");
		String binaryString = UuidBinaryUtils.toBinaryString(uuid);
		StringBuilder sb = new StringBuilder(128 + (128 / charsPerSegment));
		for (int i = 0; i < binaryString.length(); i++) {
			if (i != 0 && i % charsPerSegment == 0)
				sb.append(separator);
			sb.append(binaryString.charAt(i));
		}
		return sb.toString();
	}

	/** Converts an UUID to a binary string (list of 0 and 1). */
	public static String toBinaryString(UUID uuid) {
		Objects.requireNonNull(uuid, "UUID cannot be null");
		String most = zeroTo64Chars(Long.toBinaryString(uuid.getMostSignificantBits()));
		String least = zeroTo64Chars(Long.toBinaryString(uuid.getLeastSignificantBits()));
		String binaryString = most + least;
		assert binaryString.length() == 128;
		return binaryString;
	}

	/*
	 * LOW-LEVEL UTILITIES
	 */
	protected static String zeroTo64Chars(String str) {
		assert str.length() <= 64;
		if (str.length() < 64) {
			StringBuilder sb = new StringBuilder(64);
			for (int i = 0; i < 64 - str.length(); i++)
				sb.append('0');
			sb.append(str);
			return sb.toString();
		} else
			return str;
	}

}
