package org.argeo.api.uuid;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

/**
 * The state of a time based UUID generator, as described and discussed in
 * section 4.2.1 of RFC4122.
 * 
 * @see https://datatracker.ietf.org/doc/html/rfc4122#section-4.2.1
 */
public interface TimeUuidState {
	long MOST_SIG_VERSION1 = (1l << 12);
	long LEAST_SIG_RFC4122_VARIANT = (1l << 63);

	/** Start of the Gregorian time, used by time-based UUID (v1). */
	final static Instant GREGORIAN_START = ZonedDateTime.of(1582, 10, 15, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();

	long getLastTimestamp();

	long getClockSequence();

	long getLeastSignificantBits();

	long getMostSignificantBits();

	static boolean isNoMacAddressNodeId(byte[] nodeId) {
		return (nodeId[0] & 1) != 0;
	}
}
