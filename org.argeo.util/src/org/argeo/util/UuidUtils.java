package org.argeo.util;

import static java.lang.System.Logger.Level.DEBUG;

import java.lang.System.Logger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.BitSet;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Static utilities to simplify and extend usage of {@link UUID}. Only the RFC
 * 4122 variant (also known as Leach–Salz variant) is supported.
 * 
 * @see https://datatracker.ietf.org/doc/html/rfc4122
 */
public class UuidUtils {
	/** Nil UUID (00000000-0000-0000-0000-000000000000). */
	public final static UUID NIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

	/** Start of the Gregorian time, used by time-based UUID. */
	public final static LocalDateTime GREGORIAN_START = LocalDateTime.of(1582, 10, 15, 0, 0, 0);

	/**
	 * Standard DNS namespace ID for type 3 or 5 UUID (as defined in Appendix C of
	 * RFC4122).
	 */
	public final static UUID NS_DNS = UUID.fromString("6ba7b810-9dad-11d1-80b4-00c04fd430c8");
	/**
	 * Standard URL namespace ID for type 3 or 5 UUID (as defined in Appendix C of
	 * RFC4122).
	 */
	public final static UUID NS_URL = UUID.fromString("6ba7b811-9dad-11d1-80b4-00c04fd430c8");
	/**
	 * Standard OID namespace ID (typically an LDAP type) for type 3 or 5 UUID (as
	 * defined in Appendix C of RFC4122).
	 */
	public final static UUID NS_OID = UUID.fromString("6ba7b812-9dad-11d1-80b4-00c04fd430c8");
	/**
	 * Standard X500 namespace ID (typically an LDAP DN) for type 3 or 5 UUID (as
	 * defined in Appendix C of RFC4122).
	 */
	public final static UUID NS_X500 = UUID.fromString("6ba7b814-9dad-11d1-80b4-00c04fd430c8");

	/*
	 * INTERNAL STATIC UTILITIES
	 */
	private final static Logger logger = System.getLogger(UuidUtils.class.getName());

	private final static long MOST_SIG_VERSION1 = (1l << 12);
	private final static long LEAST_SIG_RFC4122_VARIANT = (1l << 63);
	private final static int MAX_CLOCKSEQUENCE = 16384;

	private final static SecureRandom SECURE_RANDOM;
	private final static Random UNSECURE_RANDOM;
	private final static byte[] HARDWARE_ADDRESS;

	private final static AtomicInteger CLOCK_SEQUENCE;

	/** A start timestamp to which {@link System#nanoTime()}/100 can be added. */
	private final static long START_TIMESTAMP;

	static {
		SECURE_RANDOM = new SecureRandom();
		UNSECURE_RANDOM = new Random();
		CLOCK_SEQUENCE = new AtomicInteger(SECURE_RANDOM.nextInt(MAX_CLOCKSEQUENCE));
		HARDWARE_ADDRESS = getHardwareAddress();

		long nowVm = System.nanoTime() / 100;
		Duration duration = Duration.between(GREGORIAN_START, LocalDateTime.now(ZoneOffset.UTC));
		START_TIMESTAMP = (duration.getSeconds() * 10000000 + duration.getNano() / 100) - nowVm;
	}

	/*
	 * TIME-BASED (version 1)
	 */

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

	private synchronized static long nextClockSequence() {
		int i = CLOCK_SEQUENCE.incrementAndGet();
		while (i < 0 || i >= MAX_CLOCKSEQUENCE) {
			CLOCK_SEQUENCE.set(SECURE_RANDOM.nextInt(MAX_CLOCKSEQUENCE));
			i = CLOCK_SEQUENCE.incrementAndGet();
		}
		return (long) i;
	}

	public static UUID timeUUIDwithRandomNode() {
		long timestamp = START_TIMESTAMP + System.nanoTime() / 100;
		return timeUUID(timestamp, SECURE_RANDOM);
	}

	public static UUID timeUUIDwithUnsecureRandomNode() {
		long timestamp = START_TIMESTAMP + System.nanoTime() / 100;
		return timeUUID(timestamp, UNSECURE_RANDOM);
	}

	public static UUID timeUUID(long timestamp, Random random) {
		byte[] node = new byte[6];
		random.nextBytes(node);
		node[0] = (byte) (node[0] | 1);
		long clockSequence = nextClockSequence();
		return timeUUID(timestamp, clockSequence, node);
	}

	public static UUID timeUUID() {
		long timestamp = START_TIMESTAMP + System.nanoTime() / 100;
		return timeUUID(timestamp);
	}

	public static UUID timeUUID(long timestamp) {
		if (HARDWARE_ADDRESS == null)
			return timeUUID(timestamp, SECURE_RANDOM);
		long clockSequence = nextClockSequence();
		return timeUUID(timestamp, clockSequence, HARDWARE_ADDRESS);
	}

	public static UUID timeUUID(long timestamp, NetworkInterface nic) {
		byte[] node;
		try {
			node = nic.getHardwareAddress();
		} catch (SocketException e) {
			throw new IllegalStateException("Cannot get hardware address", e);
		}
		long clockSequence = nextClockSequence();
		return timeUUID(timestamp, clockSequence, node);
	}

	public static UUID timeUUID(LocalDateTime time, long clockSequence, byte[] node) {
		Duration duration = Duration.between(GREGORIAN_START, time);
		// Number of 100 ns intervals in one second: 1000000000 / 100 = 10000000
		long timestamp = duration.getSeconds() * 10000000 + duration.getNano() / 100;
		return timeUUID(timestamp, clockSequence, node);
	}

	public static UUID timeUUID(long timestamp, long clockSequence, byte[] node) {
		Objects.requireNonNull(node, "Node array cannot be null");
		if (node.length < 6)
			throw new IllegalArgumentException("Node array must be at least 6 bytes long");

		long mostSig = MOST_SIG_VERSION1 // base for version 1 UUID
				| ((timestamp & 0xFFFFFFFFL) << 32) // time_low
				| (((timestamp >> 32) & 0xFFFFL) << 16) // time_mid
				| ((timestamp >> 48) & 0x0FFFL);// time_hi_and_version

		long leastSig = LEAST_SIG_RFC4122_VARIANT // base for Leach–Salz UUID
				| (((clockSequence & 0x3F00) >> 8) << 56) // clk_seq_hi_res
				| ((clockSequence & 0xFF) << 48) // clk_seq_low
				| (node[0] & 0xFFL) //
				| ((node[1] & 0xFFL) << 8) //
				| ((node[2] & 0xFFL) << 16) //
				| ((node[3] & 0xFFL) << 24) //
				| ((node[4] & 0xFFL) << 32) //
				| ((node[5] & 0xFFL) << 40); //
		UUID uuid = new UUID(mostSig, leastSig);

		// tests
		assert uuid.node() == BitSet.valueOf(node).toLongArray()[0];
		assert uuid.timestamp() == timestamp;
		assert uuid.clockSequence() == clockSequence
				: "uuid.clockSequence()=" + uuid.clockSequence() + " clockSequence=" + clockSequence;
		assert uuid.version() == 1;
		assert uuid.variant() == 2;
		return uuid;
	}

//	@Deprecated
//	public static UUID timeBasedUUID() {
//		return timeBasedUUID(LocalDateTime.now(ZoneOffset.UTC));
//	}
//
//	@Deprecated
//	public static UUID timeBasedRandomUUID() {
//		return timeBasedRandomUUID(LocalDateTime.now(ZoneOffset.UTC), RANDOM);
//	}
//
//	@Deprecated
//	public static UUID timeBasedUUID(LocalDateTime time) {
//		if (HARDWARE_ADDRESS == null)
//			return timeBasedRandomUUID(time, RANDOM);
//		return timeBasedUUID(time, BitSet.valueOf(HARDWARE_ADDRESS));
//	}
//
//	@Deprecated
//	public static UUID timeBasedAddressUUID(LocalDateTime time, NetworkInterface nic) throws SocketException {
//		byte[] nodeBytes = nic.getHardwareAddress();
//		BitSet node = BitSet.valueOf(nodeBytes);
//		return timeBasedUUID(time, node);
//	}
//
//	@Deprecated
//	public static UUID timeBasedRandomUUID(LocalDateTime time, Random random) {
//		byte[] nodeBytes = new byte[6];
//		random.nextBytes(nodeBytes);
//		BitSet node = BitSet.valueOf(nodeBytes);
//		// set random marker
//		node.set(0, true);
//		return timeBasedUUID(time, node);
//	}
//
//	@Deprecated
//	public static UUID timeBasedUUID(LocalDateTime time, BitSet node) {
//		// most significant
//		Duration duration = Duration.between(GREGORIAN_START, time);
//
//		// Number of 100 ns intervals in one second: 1000000000 / 100 = 10000000
//		long timeNanos = duration.getSeconds() * 10000000 + duration.getNano() / 100;
//		BitSet timeBits = BitSet.valueOf(new long[] { timeNanos });
//		assert timeBits.length() <= 60;
//
//		int clockSequence;
//		synchronized (CLOCK_SEQUENCE) {
//			clockSequence = CLOCK_SEQUENCE.incrementAndGet();
//			if (clockSequence > 16384)
//				CLOCK_SEQUENCE.set(0);
//		}
//		BitSet clockSequenceBits = BitSet.valueOf(new long[] { clockSequence });
//
//		// Build the UUID, bit by bit
//		// see https://tools.ietf.org/html/rfc4122#section-4.2.2
//		// time
//		BitSet time_low = new BitSet(32);
//		BitSet time_mid = new BitSet(16);
//		BitSet time_hi_and_version = new BitSet(16);
//
//		for (int i = 0; i < 60; i++) {
//			if (i < 32)
//				time_low.set(i, timeBits.get(i));
//			else if (i < 48)
//				time_mid.set(i - 32, timeBits.get(i));
//			else
//				time_hi_and_version.set(i - 48, timeBits.get(i));
//		}
//		// version
//		time_hi_and_version.set(12, true);
//		time_hi_and_version.set(13, false);
//		time_hi_and_version.set(14, false);
//		time_hi_and_version.set(15, false);
//
//		// clock sequence
//		BitSet clk_seq_hi_res = new BitSet(8);
//		BitSet clk_seq_low = new BitSet(8);
//		for (int i = 0; i < 8; i++) {
//			clk_seq_low.set(i, clockSequenceBits.get(i));
//		}
//		for (int i = 8; i < 14; i++) {
//			clk_seq_hi_res.set(i - 8, clockSequenceBits.get(i));
//		}
//		// variant
//		clk_seq_hi_res.set(6, false);
//		clk_seq_hi_res.set(7, true);
//
////		String str = toHexString(time_low.toLongArray()[0]) + "-" + toHexString(time_mid.toLongArray()[0]) + "-"
////				+ toHexString(time_hi_and_version.toLongArray()[0]) + "-"
////				+ toHexString(clock_seq_hi_and_reserved.toLongArray()[0]) + toHexString(clock_seq_low.toLongArray()[0])
////				+ "-" + toHexString(node.toLongArray()[0]);
////		UUID uuid = UUID.fromString(str);
//
//		BitSet uuidBits = new BitSet(128);
//		for (int i = 0; i < 128; i++) {
//			if (i < 48)
//				uuidBits.set(i, node.get(i));
//			else if (i < 56)
//				uuidBits.set(i, clk_seq_low.get(i - 48));
//			else if (i < 64)
//				uuidBits.set(i, clk_seq_hi_res.get(i - 56));
//			else if (i < 80)
//				uuidBits.set(i, time_hi_and_version.get(i - 64));
//			else if (i < 96)
//				uuidBits.set(i, time_mid.get(i - 80));
//			else
//				uuidBits.set(i, time_low.get(i - 96));
//		}
//
//		long[] uuidLongs = uuidBits.toLongArray();
//		assert uuidLongs.length == 2;
//		UUID uuid = new UUID(uuidLongs[1], uuidLongs[0]);
//
//		// tests
//		assert uuid.node() == node.toLongArray()[0];
//		assert uuid.timestamp() == timeNanos;
//		assert uuid.clockSequence() == clockSequence;
//		assert uuid.version() == 1;
//		assert uuid.variant() == 2;
//		return uuid;
//	}

	/*
	 * NAME BASED (version 3 and 5)
	 */

	public final static String MD5 = "MD5";
	public final static String SHA1 = "SHA1";

	public static UUID nameUUIDv5(UUID namespace, String name) {
		Objects.requireNonNull(namespace, "Namespace cannot be null");
		Objects.requireNonNull(name, "Name cannot be null");
		return nameUUIDv5(namespace, name.getBytes(StandardCharsets.UTF_8));
	}

	public static UUID nameUUIDv5(UUID namespace, byte[] name) {
		byte[] bytes = DigestUtils.sha1(toBytes(namespace), name);
		bytes[6] &= 0x0f;
		bytes[6] |= 0x50;// v5
		bytes[8] &= 0x3f;
		bytes[8] |= 0x80;// variant 1
		UUID result = fromBytes(bytes, 0);
		return result;
	}

	public static UUID nameUUIDv3(UUID namespace, String name) {
		Objects.requireNonNull(namespace, "Namespace cannot be null");
		Objects.requireNonNull(name, "Name cannot be null");
		return nameUUIDv3(namespace, name.getBytes(StandardCharsets.UTF_8));
	}

	public static UUID nameUUIDv3(UUID namespace, byte[] name) {
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
	public static UUID unsecureRandomUUID() {
		byte[] arr = new byte[16];
		UNSECURE_RANDOM.nextBytes(arr);
		arr[6] &= 0x0f;
		arr[6] |= 0x40;// v4
		arr[8] &= 0x3f;
		arr[8] |= 0x80;// variant 1
		return fromBytes(arr);
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

	public static byte[] toBytes(UUID uuid) {
		Objects.requireNonNull(uuid, "UUID cannot be null");
		long msb = uuid.getMostSignificantBits();
		long lsb = uuid.getLeastSignificantBits();
		return toBytes(msb, lsb);
	}

	public static void copyBytes(UUID uuid, byte[] arr, int offset) {
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
	public static String toCompact(UUID uuid) {
		return toHexString(toBytes(uuid));
	}

	public static boolean isRandom(UUID uuid) {
		return uuid.version() == 4;
	}

	public static boolean isTimeBased(UUID uuid) {
		return uuid.version() == 1;
	}

	public static boolean isTimeBasedRandom(UUID uuid) {
		if (uuid.version() == 1) {
			BitSet node = BitSet.valueOf(new long[] { uuid.node() });
			return node.get(0);
		} else
			return false;
	}

	public static boolean isNameBased(UUID uuid) {
		return uuid.version() == 3 || uuid.version() == 5;
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

	/** Singleton. */
	private UuidUtils() {
	}

	/*
	 * SMOKE TESTS
	 */

	static boolean smokeTests() throws AssertionError {

		// warm up a bit before measuring perf and logging it
		int warmUpCycles = 10;
		// int warmUpCycles = 10000000;
		if (logger.isLoggable(DEBUG))
			for (int i = 0; i < warmUpCycles; i++) {
				UUID.randomUUID();
				unsecureRandomUUID();
				timeUUID();
				timeUUIDwithRandomNode();
				nameUUIDv5(NS_DNS, "example.org");
				nameUUIDv3(NS_DNS, "example.org");
			}

		long begin;

		{
			begin = System.nanoTime();
			UUID uuid = UUID.randomUUID();
			long duration = System.nanoTime() - begin;
			assert isRandom(uuid);
			logger.log(DEBUG, () -> uuid.toString() + " in " + duration + " ns, isRandom=" + isRandom(uuid));
		}

		{
			begin = System.nanoTime();
			UUID uuid = unsecureRandomUUID();
			long duration = System.nanoTime() - begin;
			assert isRandom(uuid);
			logger.log(DEBUG, () -> uuid.toString() + " in " + duration + " ns, isRandom=" + isRandom(uuid));
		}

		{
			begin = System.nanoTime();
			UUID uuid = timeUUID();
			long duration = System.nanoTime() - begin;
			assert isTimeBased(uuid);
			logger.log(DEBUG,
					() -> uuid.toString() + " in " + duration + " ns, isTimeBasedRandom=" + isTimeBasedRandom(uuid));
		}

		{
			begin = System.nanoTime();
			UUID uuid = timeUUIDwithRandomNode();
			long duration = System.nanoTime() - begin;
			assert isTimeBasedRandom(uuid);
			logger.log(DEBUG,
					() -> uuid.toString() + " in " + duration + " ns, isTimeBasedRandom=" + isTimeBasedRandom(uuid));
		}

		{
			begin = System.nanoTime();
			UUID uuid = nameUUIDv5(NS_DNS, "example.org");
			long duration = System.nanoTime() - begin;
			assert isNameBased(uuid);
			// uuidgen --sha1 --namespace @dns --name example.org
			assert "aad03681-8b63-5304-89e0-8ca8f49461b5".equals(uuid.toString());
			logger.log(DEBUG, () -> uuid.toString() + " in " + duration + " ns, isNameBased=" + isNameBased(uuid));
		}

		{
			begin = System.nanoTime();
			UUID uuid = nameUUIDv3(NS_DNS, "example.org");
			long duration = System.nanoTime() - begin;
			assert isNameBased(uuid);
			// uuidgen --md5 --namespace @dns --name example.org
			assert "04738bdf-b25a-3829-a801-b21a1d25095b".equals(uuid.toString());
			logger.log(DEBUG, () -> uuid.toString() + " in " + duration + " ns, isNameBased=" + isNameBased(uuid));
		}
		return UuidUtils.class.desiredAssertionStatus();
	}

	public static void main(String[] args) {
		smokeTests();
	}
}
