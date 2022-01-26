package org.argeo.api.uuid;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.WARNING;

import java.lang.System.Logger;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.argeo.api.uuid.UuidFactory.TimeUuidState;

/**
 * A simple base implementation of {@link TimeUuidState}, which maintains
 * different clock sequences for each thread, based on a specified range. This
 * range is defined as clock_seq_hi (cf. RFC4122) and only clock_seq_low is
 * dynamically allocated. It means that there can be at most 256 parallel clock
 * sequences. If that limit is reached, the clock sequence which has not be used
 * for the most time is reallocated to the new thread. It is assumed that the
 * context where time uUIDs will be generated will often be using thread pools
 * (e.g. {@link ForkJoinPool#commonPool(), http server, database access, etc.)
 * and that such reallocation won't have to happen too often.
 */
public class ConcurrentTimeUuidState implements UuidFactory.TimeUuidState {
	private final static Logger logger = System.getLogger(ConcurrentTimeUuidState.class.getName());

	/** The maximum possible value of the clocksequence. */
	private final static int MAX_CLOCKSEQUENCE = 0x3F00;

	private final ClockSequenceProvider clockSequenceProvider;
	private final ThreadLocal<ConcurrentTimeUuidState.Holder> currentHolder;

	private final Instant startInstant;
	/** A start timestamp to which {@link System#nanoTime()}/100 can be added. */
	private final long startTimeStamp;

	private final Clock clock;
	private final boolean useClockForMeasurement;

	private long nodeIdBase;

	public ConcurrentTimeUuidState(SecureRandom secureRandom, Clock clock) {
		useClockForMeasurement = clock != null;
		this.clock = clock != null ? clock : Clock.systemUTC();

		Objects.requireNonNull(secureRandom);

		// compute the start reference
		startInstant = Instant.now(this.clock);
		long nowVm = nowVm();
		Duration duration = Duration.between(TimeUuid.TIMESTAMP_ZERO, startInstant);
		startTimeStamp = TimeUuid.durationToTimestamp(duration) - nowVm;

		clockSequenceProvider = new ClockSequenceProvider(secureRandom);

		// initalise a state per thread
		currentHolder = new ThreadLocal<>() {

			@Override
			protected ConcurrentTimeUuidState.Holder initialValue() {
				ConcurrentTimeUuidState.Holder value = new ConcurrentTimeUuidState.Holder();
				value.threadId = Thread.currentThread().getId();
				value.lastTimestamp = 0;
				clockSequenceProvider.newClockSequence(value);
				return value;
			}
		};
	}

	/*
	 * TIME OPERATIONS
	 */

	public long useTimestamp() {
		Holder holder = currentHolder.get();
		if (holder.clockSequence < 0) {
			clockSequenceProvider.newClockSequence(holder);
		}

		long previousTimestamp = holder.lastTimestamp;
		long now = computeNow();

		// rare case where we are sooner
		// (e.g. if system time has changed in between and we use the clock)
		if (previousTimestamp > now) {
			clockSequenceProvider.newClockSequence(holder);
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
		holder.lastTimestamp = now;
		return now;
	}

	private long computeNow() {
		if (useClockForMeasurement) {
			Duration duration = Duration.between(TimeUuid.TIMESTAMP_ZERO, Instant.now(clock));
			return TimeUuid.durationToTimestamp(duration);
		} else {
			return startTimeStamp + nowVm();
		}
	}

	private long nowVm() {
		return System.nanoTime() / 100;
	}

	@Override
	public long getClockSequence() {
		return currentHolder.get().clockSequence;
	}

	@Override
	public long getLastTimestamp() {
		return currentHolder.get().lastTimestamp;
	}

	protected void reset(long nodeIdBase, long range) {
		synchronized (clockSequenceProvider) {
			this.nodeIdBase = nodeIdBase;
			clockSequenceProvider.reset(range);
			clockSequenceProvider.notifyAll();
		}
	}

	@Override
	public long getLeastSignificantBits() {
		return currentHolder.get().leastSig;
	}

	@Override
	public long getMostSignificantBits() {
		long timestamp = useTimestamp();
		long mostSig = UuidFactory.MOST_SIG_VERSION1 | ((timestamp & 0xFFFFFFFFL) << 32) // time_low
				| (((timestamp >> 32) & 0xFFFFL) << 16) // time_mid
				| ((timestamp >> 48) & 0x0FFFL);// time_hi_and_version
		return mostSig;
	}

	/*
	 * INTERNAL CLASSSES
	 */
	private class Holder {
		private long lastTimestamp;
		private long clockSequence;
		private long threadId;

		private long leastSig;

		@Override
		public boolean equals(Object obj) {
			boolean isItself = this == obj;
			if (!isItself && clockSequence == ((Holder) obj).clockSequence)
				throw new IllegalStateException("There is another holder with the same clockSequence " + clockSequence);
			return isItself;
		}

		private void setClockSequence(long clockSequence) {
			this.clockSequence = clockSequence;
			this.leastSig = nodeIdBase // already computed node base
					| (((clockSequence & 0x3F00) >> 8) << 56) // clk_seq_hi_res
					| ((clockSequence & 0xFF) << 48); // clk_seq_low
		}

		@Override
		public String toString() {
			return "Holder " + clockSequence + ", threadId=" + threadId + ", lastTimestamp=" + lastTimestamp;
		}

	}

	private static class ClockSequenceProvider {
		/** Set to an illegal value. */
		private long range = MAX_CLOCKSEQUENCE;// this is actually clk_seq_hi
//		private int rangeSize = 256;
		private volatile long min;
		private volatile long max;
		private final AtomicLong counter = new AtomicLong(-1);

		private final SecureRandom secureRandom;

		private final Map<Holder, Long> activeHolders = Collections.synchronizedMap(new WeakHashMap<>());

		ClockSequenceProvider(SecureRandom secureRandom) {
			this.secureRandom = secureRandom;
//			reset(range);
		}

		synchronized void reset(long range) {
			// long min = secureRandom.nextInt(ConcurrentTimeUuidState.MAX_CLOCKSEQUENCE -
			// rangeSize);
			// long max = min + rangeSize;

			long min, max;
			if (range >= 0) {
				if (range > MAX_CLOCKSEQUENCE)
					throw new IllegalArgumentException("Range " + Long.toHexString(range) + " is too big");
				long previousRange = this.range;
				this.range = range & 0x3F00;
				if (this.range != range) {
					this.range = previousRange;
					throw new IllegalArgumentException(
							"Range is not properly formatted: " + range + " (0x" + Long.toHexString(range) + ")");
				}

				min = this.range;
				max = min | 0xFF;
			} else {// full range
				this.range = range;
				min = 0;
				max = MAX_CLOCKSEQUENCE;
			}
			assert min == (int) min;
			assert max == (int) max;

			// TODO rather use assertions
			if (min >= max)
				throw new IllegalArgumentException("Minimum " + min + " is bigger than maximum " + max);
			if (min < 0 || min > MAX_CLOCKSEQUENCE)
				throw new IllegalArgumentException("Minimum " + min + " is not valid");
			if (max < 0 || max > MAX_CLOCKSEQUENCE)
				throw new IllegalArgumentException("Maximum " + max + " is not valid");
			this.min = min;
			this.max = max;

			Set<Holder> active = activeHolders.keySet();
			int activeCount = active.size();
			if (activeCount > getRangeSize())
				throw new IllegalStateException(
						"There are too many holders for range [" + min + "," + max + "] : " + activeCount);

			// reset the counter with a random value in range
			long firstCount = min + secureRandom.nextInt(getRangeSize());
			counter.set(firstCount);

			// reset holders
			for (Holder holder : active) {
				// save old clocksequence?
				newClockSequence(holder);
			}
		}

		private synchronized void newClockSequence(Holder holder) {
			// Too many holders, we will remove the oldes ones
			while (activeHolders.size() > getRangeSize()) {
				long oldestTimeStamp = -1;
				Holder holderToRemove = null;
				holders: for (Holder h : activeHolders.keySet()) {
					if (h == holder)// skip the caller
						continue holders;

					if (oldestTimeStamp < 0) {
						oldestTimeStamp = h.lastTimestamp;
						holderToRemove = h;
					}
					if (h.lastTimestamp <= oldestTimeStamp) {
						oldestTimeStamp = h.lastTimestamp;
						holderToRemove = h;
					}

				}
				assert holderToRemove != null;
				long oldClockSequence = holderToRemove.clockSequence;
				holderToRemove.clockSequence = -1;
				activeHolders.remove(holderToRemove);
				if (logger.isLoggable(WARNING))
					logger.log(WARNING, "Removed " + holderToRemove + ", oldClockSequence=" + oldClockSequence);
			}

			long newClockSequence = -1;
			int tryCount = 0;// an explicit exit condition
			do {
				tryCount++;
				if (tryCount >= getRangeSize())
					throw new IllegalStateException("No more clock sequence available");

				newClockSequence = counter.incrementAndGet();
				assert newClockSequence >= 0 : "Clock sequence cannot be negative";
				if (newClockSequence > max) {
					// reset counter
					newClockSequence = min;
					counter.set(newClockSequence);
				}
			} while (activeHolders.containsValue(newClockSequence));
			// TODO use an iterator to check the values
			holder.setClockSequence(newClockSequence);
			activeHolders.put(holder, newClockSequence);
			if (logger.isLoggable(DEBUG)) {
				String clockDesc = range >= 0 ? Long.toHexString(newClockSequence & 0x00FF)
						: Long.toHexString(newClockSequence | 0x8000);
				String rangeDesc = Long.toHexString(min | 0x8000) + "-" + Long.toHexString(max | 0x8000);
				logger.log(DEBUG, "New clocksequence " + clockDesc + " for thread " + Thread.currentThread().getId()
						+ " (in range " + rangeDesc + ")");
			}
		}

		private synchronized int getRangeSize() {
			return (int) (max - min);
		}

	}

}
