package org.argeo.api.uuid;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.temporal.Temporal;
import java.util.BitSet;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Implementation of the basic RFC4122 algorithms.
 * 
 * @see https://datatracker.ietf.org/doc/html/rfc4122
 */
public abstract class AbstractUuidFactory implements UuidFactory {

	/*
	 * TIME-BASED (version 1)
	 */

	private final static long MOST_SIG_VERSION1 = (1l << 12);
	private final static long LEAST_SIG_RFC4122_VARIANT = (1l << 63);

	protected UUID newTimeUUID(long timestamp, long clockSequence, byte[] node, int offset) {
		Objects.requireNonNull(node, "Node array cannot be null");
		if (node.length < offset + 6)
			throw new IllegalArgumentException("Node array must be at least 6 bytes long");

		long mostSig = MOST_SIG_VERSION1 // base for version 1 UUID
				| ((timestamp & 0xFFFFFFFFL) << 32) // time_low
				| (((timestamp >> 32) & 0xFFFFL) << 16) // time_mid
				| ((timestamp >> 48) & 0x0FFFL);// time_hi_and_version

		long leastSig = LEAST_SIG_RFC4122_VARIANT // base for Leach–Salz UUID
				| (((clockSequence & 0x3F00) >> 8) << 56) // clk_seq_hi_res
				| ((clockSequence & 0xFF) << 48) // clk_seq_low
				| (node[offset] & 0xFFL) //
				| ((node[offset + 1] & 0xFFL) << 8) //
				| ((node[offset + 2] & 0xFFL) << 16) //
				| ((node[offset + 3] & 0xFFL) << 24) //
				| ((node[offset + 4] & 0xFFL) << 32) //
				| ((node[offset + 5] & 0xFFL) << 40); //
		UUID uuid = new UUID(mostSig, leastSig);

		// tests
		assert uuid.version() == 1;
		assert uuid.variant() == 2;
		assert uuid.node() == BitSet.valueOf(node).toLongArray()[0];
		assert uuid.timestamp() == timestamp;
		assert uuid.clockSequence() == clockSequence;
		return uuid;
	}

	protected UUID timeUUID(Temporal time, long clockSequence, byte[] node, int offset) {
		// TODO add checks
		Duration duration = Duration.between(TimeUuidState.GREGORIAN_START, time);
		// Number of 100 ns intervals in one second: 1000000000 / 100 = 10000000
		long timestamp = duration.getSeconds() * 10000000 + duration.getNano() / 100;
		return newTimeUUID(timestamp, clockSequence, node, offset);
	}

	/*
	 * NAME BASED (version 3 and 5)
	 */

	protected UUID newNameUUIDv5(UUID namespace, byte[] name) {
		Objects.requireNonNull(namespace, "Namespace cannot be null");
		Objects.requireNonNull(name, "Name cannot be null");

		byte[] bytes = sha1(toBytes(namespace), name);
		bytes[6] &= 0x0f;
		bytes[6] |= 0x50;// v5
		bytes[8] &= 0x3f;
		bytes[8] |= 0x80;// variant 1
		UUID result = fromBytes(bytes, 0);
		return result;
	}

	protected UUID newNameUUIDv3(UUID namespace, byte[] name) {
		Objects.requireNonNull(namespace, "Namespace cannot be null");
		Objects.requireNonNull(name, "Name cannot be null");

		byte[] arr = new byte[name.length + 16];
		copyBytes(namespace, arr, 0);
		System.arraycopy(name, 0, arr, 16, name.length);
		return UUID.nameUUIDFromBytes(arr);
	}

	/*
	 * RANDOM v4
	 */
	protected UUID newRandomUUID(Random random) {
		byte[] arr = new byte[16];
		random.nextBytes(arr);
		arr[6] &= 0x0f;
		arr[6] |= 0x40;// v4
		arr[8] &= 0x3f;
		arr[8] |= 0x80;// variant 1
		return fromBytes(arr);
	}

	/*
	 * DIGEST UTILITIES
	 */

	private final static String MD5 = "MD5";
	private final static String SHA1 = "SHA1";

	protected byte[] sha1(byte[]... bytes) {
		MessageDigest digest = getSha1Digest();
		for (byte[] arr : bytes)
			digest.update(arr);
		byte[] checksum = digest.digest();
		return checksum;
	}

	protected byte[] md5(byte[]... bytes) {
		MessageDigest digest = getMd5Digest();
		for (byte[] arr : bytes)
			digest.update(arr);
		byte[] checksum = digest.digest();
		return checksum;
	}

	protected MessageDigest getSha1Digest() {
		return getDigest(SHA1);
	}

	protected MessageDigest getMd5Digest() {
		return getDigest(MD5);
	}

	private MessageDigest getDigest(String name) {
		try {
			return MessageDigest.getInstance(name);
		} catch (NoSuchAlgorithmException e) {
			throw new UnsupportedOperationException(name + " digest is not avalaible", e);
		}
	}

	/*
	 * UTILITIES
	 */
	/**
	 * Convert bytes to an UUID. Byte array must not be null and be exactly of
	 * length 16.
	 */
	protected UUID fromBytes(byte[] data) {
		Objects.requireNonNull(data, "Byte array must not be null");
		if (data.length != 16)
			throw new IllegalArgumentException("Byte array as length " + data.length);
		return fromBytes(data, 0);
	}

	/**
	 * Convert bytes to an UUID, starting to read the array at this offset.
	 */
	protected UUID fromBytes(byte[] data, int offset) {
		Objects.requireNonNull(data, "Byte array cannot be null");
		long msb = 0;
		long lsb = 0;
		for (int i = offset; i < 8 + offset; i++)
			msb = (msb << 8) | (data[i] & 0xff);
		for (int i = 8 + offset; i < 16 + offset; i++)
			lsb = (lsb << 8) | (data[i] & 0xff);
		return new UUID(msb, lsb);
	}

	protected long longFromBytes(byte[] data) {
		long msb = 0;
		for (int i = 0; i < data.length; i++)
			msb = (msb << 8) | (data[i] & 0xff);
		return msb;
	}

	protected byte[] toBytes(UUID uuid) {
		Objects.requireNonNull(uuid, "UUID cannot be null");
		long msb = uuid.getMostSignificantBits();
		long lsb = uuid.getLeastSignificantBits();
		return toBytes(msb, lsb);
	}

	protected void copyBytes(UUID uuid, byte[] arr, int offset) {
		Objects.requireNonNull(uuid, "UUID cannot be null");
		long msb = uuid.getMostSignificantBits();
		long lsb = uuid.getLeastSignificantBits();
		copyBytes(msb, lsb, arr, offset);
	}

	/**
	 * Converts an UUID hex representation without '-' to the standard form (with
	 * '-').
	 */
	public String compactToStd(String compact) {
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
	 * Converts an UUID hex representation without '-' to an {@link UUID}.
	 */
	public UUID fromCompact(String compact) {
		return UUID.fromString(compactToStd(compact));
	}

	/** To a 32 characters hex string without '-'. */
	public String toCompact(UUID uuid) {
		return toHexString(toBytes(uuid));
	}

	final protected static char[] hexArray = "0123456789abcdef".toCharArray();

	/** Convert two longs to a byte array with length 16. */
	protected byte[] toBytes(long long1, long long2) {
		byte[] result = new byte[16];
		for (int i = 0; i < 8; i++)
			result[i] = (byte) ((long1 >> ((7 - i) * 8)) & 0xff);
		for (int i = 8; i < 16; i++)
			result[i] = (byte) ((long2 >> ((15 - i) * 8)) & 0xff);
		return result;
	}

	protected void copyBytes(long long1, long long2, byte[] arr, int offset) {
		assert arr.length >= 16 + offset;
		for (int i = offset; i < 8 + offset; i++)
			arr[i] = (byte) ((long1 >> ((7 - i) * 8)) & 0xff);
		for (int i = 8 + offset; i < 16 + offset; i++)
			arr[i] = (byte) ((long2 >> ((15 - i) * 8)) & 0xff);
	}

	/** Converts a byte array to an hex String. */
	protected String toHexString(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	protected byte[] toNodeIdBytes(byte[] source, int offset) {
		if (source == null)
			return null;
		if (offset < 0 || offset + 6 > source.length)
			throw new ArrayIndexOutOfBoundsException(offset);
		byte[] nodeId = new byte[6];
		System.arraycopy(source, offset, nodeId, 0, 6);
		return nodeId;
	}

	/*
	 * STATIC UTILITIES
	 */
	/**
	 * Converts an UUID to a binary string (list of 0 and 1), with a separator to
	 * make it more readable.
	 */
	public static String toBinaryString(UUID uuid, int charsPerSegment, char separator) {
		Objects.requireNonNull(uuid, "UUID cannot be null");
		String binaryString = toBinaryString(uuid);
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

	/**
	 * Force this node id to be identified as no MAC address.
	 * 
	 * @see https://datatracker.ietf.org/doc/html/rfc4122#section-4.5
	 */
	public static void forceToNoMacAddress(byte[] nodeId, int offset) {
		assert nodeId != null && offset < nodeId.length;
		nodeId[offset] = (byte) (nodeId[offset] | 1);
	}

	private static String zeroTo64Chars(String str) {
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
