package org.argeo.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.BitSet;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utilities to simplify and extends usage of {@link UUID}. Only the RFC 4122
 * variant (also known as Leach–Salz variant) is supported.
 */
public class UuidUtils {
	/** Nil UUID (00000000-0000-0000-0000-000000000000). */
	public final static UUID NIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");
	public final static LocalDateTime GREGORIAN_START = LocalDateTime.of(1582, 10, 15, 0, 0, 0);

	private final static long MOST_SIG_VERSION1 = (1l << 12);
	private final static long LEAST_SIG_RFC4122_VARIANT = (1l << 63);

	private final static SecureRandom RANDOM;
	private final static AtomicInteger CLOCK_SEQUENCE;
	private final static byte[] HARDWARE_ADDRESS;
	/** A start timestamp to which {@link System#nanoTime()}/100 can be added. */
	private final static long START_TIMESTAMP;
	static {
		RANDOM = new SecureRandom();
		CLOCK_SEQUENCE = new AtomicInteger(RANDOM.nextInt(16384));
		HARDWARE_ADDRESS = getHardwareAddress();

		long nowVm = System.nanoTime() / 100;
		Duration duration = Duration.between(GREGORIAN_START, LocalDateTime.now(ZoneOffset.UTC));
		START_TIMESTAMP = (duration.getSeconds() * 10000000 + duration.getNano() / 100) - nowVm;
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

	public static UUID timeUUIDwithRandomNode() {
		long timestamp = START_TIMESTAMP + System.nanoTime() / 100;
		return timeUUID(timestamp, RANDOM);
	}

	public static UUID timeUUID(long timestamp, Random random) {
		byte[] node = new byte[6];
		random.nextBytes(node);
		node[0] = (byte) (node[0] | 1);
		long clockSequence = CLOCK_SEQUENCE.incrementAndGet();
		return timeUUID(timestamp, clockSequence, node);
	}

	public static UUID timeUUID() {
		long timestamp = START_TIMESTAMP + System.nanoTime() / 100;
		return timeUUID(timestamp);
	}

	public static UUID timeUUID(long timestamp) {
		if (HARDWARE_ADDRESS == null)
			return timeUUID(timestamp, RANDOM);
		long clockSequence = CLOCK_SEQUENCE.incrementAndGet();
		return timeUUID(timestamp, clockSequence, HARDWARE_ADDRESS);
	}

	public static UUID timeUUID(long timestamp, NetworkInterface nic) {
		byte[] node;
		try {
			node = nic.getHardwareAddress();
		} catch (SocketException e) {
			throw new IllegalStateException("Cannot get hardware address", e);
		}
		long clockSequence = CLOCK_SEQUENCE.incrementAndGet();
		return timeUUID(timestamp, clockSequence, node);
	}

	public static UUID timeUUID(LocalDateTime time, long clockSequence, byte[] node) {
		Duration duration = Duration.between(GREGORIAN_START, time);
		// Number of 100 ns intervals in one second: 1000000000 / 100 = 10000000
		long timestamp = duration.getSeconds() * 10000000 + duration.getNano() / 100;
		return timeUUID(timestamp, clockSequence, node);
	}

	public static UUID timeUUID(long timestamp, long clockSequence, byte[] node) {
		assert node.length >= 6;

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
//		for (int i = 0; i < 6; i++) {
//			leastSig = leastSig | ((node[i] & 0xFFL) << (8 * i));
//		}
		UUID uuid = new UUID(mostSig, leastSig);

		// tests
		assert uuid.node() == BitSet.valueOf(node).toLongArray()[0];
		assert uuid.timestamp() == timestamp;
		assert uuid.clockSequence() == clockSequence;
		assert uuid.version() == 1;
		assert uuid.variant() == 2;
		return uuid;
	}

	@Deprecated
	public static UUID timeBasedUUID() {
		return timeBasedUUID(LocalDateTime.now(ZoneOffset.UTC));
	}

	@Deprecated
	public static UUID timeBasedRandomUUID() {
		return timeBasedRandomUUID(LocalDateTime.now(ZoneOffset.UTC), RANDOM);
	}

	@Deprecated
	public static UUID timeBasedUUID(LocalDateTime time) {
		if (HARDWARE_ADDRESS == null)
			return timeBasedRandomUUID(time, RANDOM);
		return timeBasedUUID(time, BitSet.valueOf(HARDWARE_ADDRESS));
	}

	@Deprecated
	public static UUID timeBasedAddressUUID(LocalDateTime time, NetworkInterface nic) throws SocketException {
		byte[] nodeBytes = nic.getHardwareAddress();
		BitSet node = BitSet.valueOf(nodeBytes);
		return timeBasedUUID(time, node);
	}

	@Deprecated
	public static UUID timeBasedRandomUUID(LocalDateTime time, Random random) {
		byte[] nodeBytes = new byte[6];
		random.nextBytes(nodeBytes);
		BitSet node = BitSet.valueOf(nodeBytes);
		// set random marker
		node.set(0, true);
		return timeBasedUUID(time, node);
	}

	@Deprecated
	public static UUID timeBasedUUID(LocalDateTime time, BitSet node) {
		// most significant
		Duration duration = Duration.between(GREGORIAN_START, time);

		// Number of 100 ns intervals in one second: 1000000000 / 100 = 10000000
		long timeNanos = duration.getSeconds() * 10000000 + duration.getNano() / 100;
		BitSet timeBits = BitSet.valueOf(new long[] { timeNanos });
		assert timeBits.length() <= 60;

		int clockSequence;
		synchronized (CLOCK_SEQUENCE) {
			clockSequence = CLOCK_SEQUENCE.incrementAndGet();
			if (clockSequence > 16384)
				CLOCK_SEQUENCE.set(0);
		}
		BitSet clockSequenceBits = BitSet.valueOf(new long[] { clockSequence });

		// Build the UUID, bit by bit
		// see https://tools.ietf.org/html/rfc4122#section-4.2.2
		// time
		BitSet time_low = new BitSet(32);
		BitSet time_mid = new BitSet(16);
		BitSet time_hi_and_version = new BitSet(16);

		for (int i = 0; i < 60; i++) {
			if (i < 32)
				time_low.set(i, timeBits.get(i));
			else if (i < 48)
				time_mid.set(i - 32, timeBits.get(i));
			else
				time_hi_and_version.set(i - 48, timeBits.get(i));
		}
		// version
		time_hi_and_version.set(12, true);
		time_hi_and_version.set(13, false);
		time_hi_and_version.set(14, false);
		time_hi_and_version.set(15, false);

		// clock sequence
		BitSet clk_seq_hi_res = new BitSet(8);
		BitSet clk_seq_low = new BitSet(8);
		for (int i = 0; i < 8; i++) {
			clk_seq_low.set(i, clockSequenceBits.get(i));
		}
		for (int i = 8; i < 14; i++) {
			clk_seq_hi_res.set(i - 8, clockSequenceBits.get(i));
		}
		// variant
		clk_seq_hi_res.set(6, false);
		clk_seq_hi_res.set(7, true);

//		String str = toHexString(time_low.toLongArray()[0]) + "-" + toHexString(time_mid.toLongArray()[0]) + "-"
//				+ toHexString(time_hi_and_version.toLongArray()[0]) + "-"
//				+ toHexString(clock_seq_hi_and_reserved.toLongArray()[0]) + toHexString(clock_seq_low.toLongArray()[0])
//				+ "-" + toHexString(node.toLongArray()[0]);
//		UUID uuid = UUID.fromString(str);

		BitSet uuidBits = new BitSet(128);
		for (int i = 0; i < 128; i++) {
			if (i < 48)
				uuidBits.set(i, node.get(i));
			else if (i < 56)
				uuidBits.set(i, clk_seq_low.get(i - 48));
			else if (i < 64)
				uuidBits.set(i, clk_seq_hi_res.get(i - 56));
			else if (i < 80)
				uuidBits.set(i, time_hi_and_version.get(i - 64));
			else if (i < 96)
				uuidBits.set(i, time_mid.get(i - 80));
			else
				uuidBits.set(i, time_low.get(i - 96));
		}

		long[] uuidLongs = uuidBits.toLongArray();
		assert uuidLongs.length == 2;
		UUID uuid = new UUID(uuidLongs[1], uuidLongs[0]);

		// tests
		assert uuid.node() == node.toLongArray()[0];
		assert uuid.timestamp() == timeNanos;
		assert uuid.clockSequence() == clockSequence;
		assert uuid.version() == 1;
		assert uuid.variant() == 2;
		return uuid;
	}

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

	public static UUID compactToUuid(String compact) {
		return UUID.fromString(compactToStd(compact));
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

	/** Singleton. */
	private UuidUtils() {
	}

	public final static void main(String[] args) throws Exception {
		UUID uuid;

//		uuid = compactToUuid("996b1f5122de4b2f94e49168d32f22d1");
//		System.out.println(uuid.toString() + ", isRandom=" + isRandom(uuid));

		// warm up before measuring perf
		for (int i = 0; i < 10; i++) {
			UUID.randomUUID();
			timeUUID();
			timeUUIDwithRandomNode();
			timeBasedRandomUUID();
			timeBasedUUID();
		}

		long begin;
		long duration;

		begin = System.nanoTime();
		uuid = UUID.randomUUID();
		duration = System.nanoTime() - begin;
		System.out.println(uuid.toString() + " in " + duration + " ns, isRandom=" + isRandom(uuid));

		begin = System.nanoTime();
		uuid = timeUUID();
		duration = System.nanoTime() - begin;
		System.out.println(uuid.toString() + " in " + duration + " ns, isTimeBasedRandom=" + isTimeBasedRandom(uuid));

		begin = System.nanoTime();
		uuid = timeUUIDwithRandomNode();
		duration = System.nanoTime() - begin;
		System.out.println(uuid.toString() + " in " + duration + " ns, isTimeBasedRandom=" + isTimeBasedRandom(uuid));

		begin = System.nanoTime();
		uuid = timeBasedUUID();
		duration = System.nanoTime() - begin;
		System.out.println(uuid.toString() + " in " + duration + " ns, isTimeBasedRandom=" + isTimeBasedRandom(uuid));

		begin = System.nanoTime();
		uuid = timeBasedRandomUUID();
		duration = System.nanoTime() - begin;
		System.out.println(uuid.toString() + " in " + duration + " ns, isTimeBasedRandom=" + isTimeBasedRandom(uuid));
//		System.out.println(toBinaryString(uuid, 8, ' '));
//		System.out.println(toBinaryString(uuid, 16, '\n'));
	}
}
