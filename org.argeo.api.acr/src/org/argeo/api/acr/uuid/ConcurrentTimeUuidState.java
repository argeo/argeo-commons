package org.argeo.api.acr.uuid;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * A simple base implementation of {@link TimeUuidState}, which maintains
 * different clock sequences for each thread.
 */
public class ConcurrentTimeUuidState implements TimeUuidState {
	/** The maximum possible value of the clocksequence. */
	private final static int MAX_CLOCKSEQUENCE = 16384;

	private final ThreadLocal<Holder> holder;

	private final Instant startInstant;
	/** A start timestamp to which {@link System#nanoTime()}/100 can be added. */
	private final long startTimeStamp;

	private final Clock clock;
	private final boolean useClockForMeasurement;

	private final SecureRandom secureRandom;

	public ConcurrentTimeUuidState(SecureRandom secureRandom, Clock clock) {
		useClockForMeasurement = clock != null;
		this.clock = clock != null ? clock : Clock.systemUTC();

		Objects.requireNonNull(secureRandom);
		this.secureRandom = secureRandom;

		// compute the start reference
		startInstant = Instant.now(this.clock);
		long nowVm = nowVm();
		Duration duration = Duration.between(TimeUuidState.GREGORIAN_START, startInstant);
		startTimeStamp = durationToUuidTimestamp(duration) - nowVm;

		// initalise a state per thread
		holder = new ThreadLocal<>() {

			@Override
			protected Holder initialValue() {
				Holder value = new Holder();
				value.lastTimestamp = startTimeStamp;
				value.clockSequence = newClockSequence();
				return value;
			}
		};
	}

	/*
	 * TIME OPERATIONS
	 */

	public long useTimestamp() {
		long previousTimestamp = holder.get().lastTimestamp;
		long now = computeNow();

		// rare case where we are sooner
		// (e.g. if system time has changed in between and we use the clock)
		if (previousTimestamp > now) {
			long newClockSequence = newClockSequence();
			for (int i = 0; i < 64; i++) {
				if (newClockSequence != holder.get().clockSequence)
					break;
				newClockSequence = newClockSequence();
			}
			if (newClockSequence != holder.get().clockSequence)
				throw new IllegalStateException("Cannot change clock sequence");
			holder.get().clockSequence = newClockSequence;
		}

		// very unlikely case where it took less than 100ns between both
		if (previousTimestamp == now) {
			try {
				Thread.sleep(0, 100);
			} catch (InterruptedException e) {
				// silent
			}
			now = computeNow();
			assert previousTimestamp != now;
		}
		holder.get().lastTimestamp = now;
		return now;
	}

	protected long computeNow() {
		if (useClockForMeasurement) {
			Duration duration = Duration.between(TimeUuidState.GREGORIAN_START, Instant.now(clock));
			return durationToUuidTimestamp(duration);
		} else {
			return startTimeStamp + nowVm();
		}
	}

	private long nowVm() {
		return System.nanoTime() / 100;
	}

	protected long durationToUuidTimestamp(Duration duration) {
		return (duration.getSeconds() * 10000000 + duration.getNano() / 100);
	}

	/*
	 * STATE OPERATIONS
	 */

	protected long newClockSequence() {
		return secureRandom.nextInt(ConcurrentTimeUuidState.MAX_CLOCKSEQUENCE);
	}

	/*
	 * ACCESSORS
	 */

//	@Override
//	public byte[] getNodeId() {
//		byte[] arr = new byte[6];
//		System.arraycopy(holder.get().nodeId, 0, arr, 0, 6);
//		return arr;
//	}

	@Override
	public long getClockSequence() {
		return holder.get().clockSequence;
	}
}
