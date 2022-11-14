package org.argeo.api.uuid;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * A time based UUID, whose content can therefore be usefully interpreted as
 * time and node identifier information.
 */
public class TimeUuid extends TypedUuid {
	private static final long serialVersionUID = APM.SERIAL;
	/**
	 * Start of the Gregorian time on October 15th 1582, equivalent to
	 * <code>{@link UUID#timestamp()} == 0</code>.
	 */
	public final static Instant TIMESTAMP_ZERO = ZonedDateTime.of(1582, 10, 15, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();

	/** Constructor based on a {@link UUID}. */
	public TimeUuid(UUID uuid) {
		super(uuid);
		if (uuid.version() != 1 && uuid.variant() != 2)
			throw new IllegalArgumentException("The provided UUID is not a time based UUID.");
	}

	/** {@link UUID#timestamp()} as an {@link Instant}. */
	public final Instant getInstant() {
		long timestamp = uuid.timestamp();
		return TIMESTAMP_ZERO.plus(timestampDifferenceToDuration(timestamp));
	}

	/** {@link UUID#node()} as an hex string. */
	public final String getNodeId() {
		return Long.toHexString(uuid.node());
	}

	/** {@link UUID#clockSequence()} as an hex string. */
	public final String getClockSequence() {
		return Long.toHexString(uuid.clockSequence());
	}

	/**
	 * Always returns <code>false</code> since time UUIDs are by definition not
	 * opaque.
	 */
	@Override
	public final boolean isOpaque() {
		return false;
	}

	/*
	 * STATIC UTILITIES
	 */
	/** Converts from duration in the time UUID timestamp format. */
	public static Duration timestampDifferenceToDuration(long timestampDifference) {
		long seconds = timestampDifference / 10000000;
		long nano = (timestampDifference % 10000000) * 100;
		return Duration.ofSeconds(seconds, nano);
	}

	/**
	 * A duration expressed in the time UUID timestamp format based on units of 100
	 * ns.
	 */
	public static long durationToTimestamp(Duration duration) {
		return (duration.getSeconds() * 10000000 + duration.getNano() / 100);
	}

	/**
	 * An instant expressed in the time UUID timestamp format based on units of 100
	 * ns since {@link #TIMESTAMP_ZERO}.
	 */
	public static long instantToTimestamp(Instant instant) {
		Duration duration = Duration.between(TimeUuid.TIMESTAMP_ZERO, instant);
		return durationToTimestamp(duration);
	}

	/**
	 * Crate a time UUID with this instant as timestamp and clock and node id set to
	 * zero.
	 */
	public static UUID fromInstant(Instant instant) {
		long timestamp = instantToTimestamp(instant);
		long mostSig = toMostSignificantBits(timestamp);
		UUID uuid = new UUID(mostSig, UuidFactory.LEAST_SIG_RFC4122_VARIANT);
		return uuid;
	}

	/** Convert timestamp in UUID format to most significant bits of a time UUID. */
	static long toMostSignificantBits(long timestamp) {
		long mostSig = UuidFactory.MOST_SIG_VERSION1 | ((timestamp & 0xFFFFFFFFL) << 32) // time_low
				| (((timestamp >> 32) & 0xFFFFL) << 16) // time_mid
				| ((timestamp >> 48) & 0x0FFFL);// time_hi_and_version
		return mostSig;
	}
}
