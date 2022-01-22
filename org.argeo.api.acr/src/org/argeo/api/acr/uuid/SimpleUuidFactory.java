package org.argeo.api.acr.uuid;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.WARNING;

import java.lang.System.Logger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.DrbgParameters;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.temporal.Temporal;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

/**
 * Simple implementation of an {@link UuidFactory}, which can be used as a base
 * class for more optimised implementations.
 * 
 * @see https://datatracker.ietf.org/doc/html/rfc4122
 */
public class SimpleUuidFactory implements UuidFactory {
	private final static Logger logger = System.getLogger(SimpleUuidFactory.class.getName());
	public final static UuidFactory DEFAULT = new SimpleUuidFactory(null, -1, null);
//	private final static int MAX_CLOCKSEQUENCE = 16384;

	private SecureRandom secureRandom;
	private final byte[] hardwareAddress;

//	private final AtomicInteger clockSequence;

	/** A start timestamp to which {@link System#nanoTime()}/100 can be added. */
//	private final long startTimeStamp;

	private final TimeUuidState macAddressTimeUuidState;
	private final TimeUuidState defaultTimeUuidState;


	public SimpleUuidFactory(byte[] nodeId, int offset, Clock clock) {
		try {
			secureRandom = SecureRandom.getInstance("DRBG",
					DrbgParameters.instantiation(256, DrbgParameters.Capability.PR_AND_RESEED, "UUID".getBytes()));
		} catch (NoSuchAlgorithmException e) {
			try {
				logger.log(DEBUG, "DRBG secure random not found, using strong");
				secureRandom = SecureRandom.getInstanceStrong();
			} catch (NoSuchAlgorithmException e1) {
				logger.log(WARNING, "No strong secure random was found, using default");
				secureRandom = new SecureRandom();
			}
		}

//		clockSequence = new AtomicInteger(secureRandom.nextInt(MAX_CLOCKSEQUENCE));
		hardwareAddress = getHardwareAddress();

		macAddressTimeUuidState = hardwareAddress != null
				? new ConcurrentTimeUuidState(hardwareAddress, 0, secureRandom, clock)
				: null;
		defaultTimeUuidState = nodeId != null ? new ConcurrentTimeUuidState(nodeId, offset, secureRandom, clock)
				: macAddressTimeUuidState != null ? macAddressTimeUuidState
						// we use random as a last resort
						: new ConcurrentTimeUuidState(null, -1, secureRandom, clock);

		// GREGORIAN_START = ZonedDateTime.of(1582, 10, 15, 0, 0, 0, 0, ZoneOffset.UTC);
//		Duration duration = Duration.between(TimeUuidState.GREGORIAN_START, Instant.now());
//		long nowVm = System.nanoTime() / 100;
//		startTimeStamp = (duration.getSeconds() * 10000000 + duration.getNano() / 100) - nowVm;
	}

	/*
	 * TIME-BASED (version 1)
	 */

	private final static long MOST_SIG_VERSION1 = (1l << 12);
	private final static long LEAST_SIG_RFC4122_VARIANT = (1l << 63);

	protected UUID timeUUID(long timestamp, long clockSequence, byte[] node, int offset) {
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
//		assert uuid.node() == BitSet.valueOf(node).toLongArray()[0];
		//assert uuid.node() == longFromBytes(node);
		assert uuid.timestamp() == timestamp;
		assert uuid.clockSequence() == clockSequence
				: "uuid.clockSequence()=" + uuid.clockSequence() + " clockSequence=" + clockSequence;
		assert uuid.version() == 1;
		assert uuid.variant() == 2;
		return uuid;
	}

	@Override
	public UUID timeUUIDwithMacAddress() {
		if (macAddressTimeUuidState == null)
			throw new UnsupportedOperationException("No MAC address is available");
//		long timestamp = startTimeStamp + System.nanoTime() / 100;
		return timeUUID(macAddressTimeUuidState.useTimestamp(), macAddressTimeUuidState.getClockSequence(),
				macAddressTimeUuidState.getNodeId(), 0);
	}

//	public UUID timeUUID(long timestamp, Random random) {
//		byte[] node = new byte[6];
//		random.nextBytes(node);
//		node[0] = (byte) (node[0] | 1);
////		long clockSequence = nextClockSequence();
//		return timeUUID(timestamp, macAddressTimeUuidState.getClockSequence(), node, 0);
//	}

	@Override
	public UUID timeUUID() {
//		long timestamp = startTimeStamp + System.nanoTime() / 100;
//		return timeUUID(timeUuidState.useTimestamp());
//	}
//
//	public UUID timeUUID(long timestamp) {
//		if (hardwareAddress == null)
//			return timeUUID(timestamp, secureRandom);
//		long clockSequence = nextClockSequence();
		return timeUUID(defaultTimeUuidState.useTimestamp(), defaultTimeUuidState.getClockSequence(),
				defaultTimeUuidState.getNodeId(), 0);
	}

//	public UUID timeUUID(long timestamp, NetworkInterface nic) {
//		byte[] node;
//		try {
//			node = nic.getHardwareAddress();
//		} catch (SocketException e) {
//			throw new IllegalStateException("Cannot get hardware address", e);
//		}
////		long clockSequence = nextClockSequence();
//		return timeUUID(timestamp, macAddressTimeUuidState.getClockSequence(), node, 0);
//	}

	public UUID timeUUID(Temporal time, long clockSequence, byte[] node) {
		Duration duration = Duration.between(TimeUuidState.GREGORIAN_START, time);
		// Number of 100 ns intervals in one second: 1000000000 / 100 = 10000000
		long timestamp = duration.getSeconds() * 10000000 + duration.getNano() / 100;
		return timeUUID(timestamp, clockSequence, node, 0);
	}

	private static byte[] getHardwareAddress() {
		InetAddress localHost;
		try {
			localHost = InetAddress.getLocalHost();
			try {
				NetworkInterface nic = NetworkInterface.getByInetAddress(localHost);
				return nic.getHardwareAddress();
			} catch (SocketException e) {
				return null;
			}
		} catch (UnknownHostException e) {
			return null;
		}

	}

//	private synchronized long nextClockSequence() {
//		int i = clockSequence.incrementAndGet();
//		while (i < 0 || i >= MAX_CLOCKSEQUENCE) {
//			clockSequence.set(secureRandom.nextInt(MAX_CLOCKSEQUENCE));
//			i = clockSequence.incrementAndGet();
//		}
//		return (long) i;
//	}

	/*
	 * NAME BASED (version 3 and 5)
	 */

//	private final static String MD5 = "MD5";
	private final static String SHA1 = "SHA1";

	@Override
	public UUID nameUUIDv5(UUID namespace, byte[] name) {
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

	@Override
	public UUID nameUUIDv3(UUID namespace, byte[] name) {
		Objects.requireNonNull(namespace, "Namespace cannot be null");
		Objects.requireNonNull(name, "Name cannot be null");

		byte[] arr = new byte[name.length + 16];
		copyBytes(namespace, arr, 0);
		System.arraycopy(name, 0, arr, 16, name.length);
		return UUID.nameUUIDFromBytes(arr);
	}

	static byte[] sha1(byte[]... bytes) {
		try {
			MessageDigest digest = MessageDigest.getInstance(SHA1);
			for (byte[] arr : bytes)
				digest.update(arr);
			byte[] checksum = digest.digest();
			return checksum;
		} catch (NoSuchAlgorithmException e) {
			throw new UnsupportedOperationException("SHA1 is not avalaible", e);
		}
	}

	/*
	 * RANDOM v4
	 */
	@Override
	public UUID randomUUID(Random random) {
		byte[] arr = new byte[16];
		random.nextBytes(arr);
		arr[6] &= 0x0f;
		arr[6] |= 0x40;// v4
		arr[8] &= 0x3f;
		arr[8] |= 0x80;// variant 1
		return fromBytes(arr);
	}

	@Override
	public UUID randomUUID() {
		return randomUUID(secureRandom);
		// return UuidFactory.super.randomUUID();
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
	 * Converts an UUID to a binary string (list of 0 and 1), with a separator to
	 * make it more readable.
	 */
	public static String toBinaryString(UUID uuid, int charsPerSegment, char separator) {
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
		String most = zeroTo64Chars(Long.toBinaryString(uuid.getMostSignificantBits()));
		String least = zeroTo64Chars(Long.toBinaryString(uuid.getLeastSignificantBits()));
		String binaryString = most + least;
		assert binaryString.length() == 128;
		return binaryString;
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
	 * Converts an UUID hex representation without '-' to an {@link UUID}.
	 */
	public static UUID fromCompact(String compact) {
		return UUID.fromString(compactToStd(compact));
	}

	/** To a 32 characters hex string without '-'. */
	public String toCompact(UUID uuid) {
		return toHexString(toBytes(uuid));
	}

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
}
