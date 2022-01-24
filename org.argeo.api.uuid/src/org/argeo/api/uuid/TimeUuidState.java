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

	/** Start of the Gregorian time, used by time-based UUID (v1). */
	final static Instant GREGORIAN_START = ZonedDateTime.of(1582, 10, 15, 0, 0, 0, 0, ZoneOffset.UTC).toInstant();

	long useTimestamp();

	long getClockSequence();

	static boolean isNoMacAddressNodeId(byte[] nodeId) {
		return (nodeId[0] & 1) != 0;
	}

	static class Holder {
		long lastTimestamp;
		long clockSequence;

		public long getLastTimestamp() {
			return lastTimestamp;
		}

		public void setLastTimestamp(long lastTimestamp) {
			this.lastTimestamp = lastTimestamp;
		}

		public long getClockSequence() {
			return clockSequence;
		}

		public void setClockSequence(long clockSequence) {
			this.clockSequence = clockSequence;
		}

	}
}
