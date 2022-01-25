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
		Duration duration = Duration.between(TimeUuid.TIMESTAMP_ZERO, time);
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

		byte[] bytes = sha1(UuidBinaryUtils.toBytes(namespace), name);
		bytes[6] &= 0x0f;
		bytes[6] |= 0x50;// v5
		bytes[8] &= 0x3f;
		bytes[8] |= 0x80;// variant 1
		UUID result = UuidBinaryUtils.fromBytes(bytes, 0);
		return result;
	}

	protected UUID newNameUUIDv3(UUID namespace, byte[] name) {
		Objects.requireNonNull(namespace, "Namespace cannot be null");
		Objects.requireNonNull(name, "Name cannot be null");

		byte[] arr = new byte[name.length + 16];
		UuidBinaryUtils.copyBytes(namespace, arr, 0);
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
		return UuidBinaryUtils.fromBytes(arr);
	}

	/*
	 * SPI UTILITIES
	 */
	/** Guarantees that a byte array of length 6 will be returned. */
	protected static byte[] toNodeIdBytes(byte[] source, int offset) {
		if (source == null)
			return null;
		if (offset < 0 || offset + 6 > source.length)
			throw new ArrayIndexOutOfBoundsException(offset);
		byte[] nodeId = new byte[6];
		System.arraycopy(source, offset, nodeId, 0, 6);
		return nodeId;
	}

	/**
	 * Force this node id to be identified as no MAC address.
	 * 
	 * @see https://datatracker.ietf.org/doc/html/rfc4122#section-4.5
	 */
	protected static void forceToNoMacAddress(byte[] nodeId, int offset) {
		assert nodeId != null && offset < nodeId.length;
		nodeId[offset] = (byte) (nodeId[offset] | 1);
	}

	/*
	 * DIGEST UTILITIES
	 */

	private final static String MD5 = "MD5";
	private final static String SHA1 = "SHA1";

	protected static byte[] sha1(byte[]... bytes) {
		MessageDigest digest = getSha1Digest();
		for (byte[] arr : bytes)
			digest.update(arr);
		byte[] checksum = digest.digest();
		return checksum;
	}

	protected static byte[] md5(byte[]... bytes) {
		MessageDigest digest = getMd5Digest();
		for (byte[] arr : bytes)
			digest.update(arr);
		byte[] checksum = digest.digest();
		return checksum;
	}

	protected static MessageDigest getSha1Digest() {
		return getDigest(SHA1);
	}

	protected static MessageDigest getMd5Digest() {
		return getDigest(MD5);
	}

	private static MessageDigest getDigest(String name) {
		try {
			return MessageDigest.getInstance(name);
		} catch (NoSuchAlgorithmException e) {
			throw new UnsupportedOperationException(name + " digest is not avalaible", e);
		}
	}

}
