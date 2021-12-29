package org.argeo.init.logging;

import java.io.Serializable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** A log entry with equals semantics based on an incremental long sequence. */
class ThinLogEntry implements Serializable {
	private static final long serialVersionUID = 5915553445193937270L;

	private final static AtomicLong next = new AtomicLong(0l);

//	private final transient Logger logger;

	private final long sequence;
	private final String loggerName;
	private final Instant instant;
	private final Level level;
	private final String message;
	private final Optional<Throwable> throwable;
	private final Optional<StackTraceElement> callLocation;

	protected ThinLogEntry(Logger logger, Level level, String message, Instant instant, Throwable e,
			StackTraceElement callLocation) {
		// NOTE: 0 is never allocated, in order to have a concept of "null" entry
		sequence = next.incrementAndGet();
//		this.logger = logger;

		this.loggerName = Objects.requireNonNull(logger).getName();
		this.instant = Objects.requireNonNull(instant);
		this.level = level;
		this.message = message;
		this.throwable = Optional.ofNullable(e);
		this.callLocation = Optional.ofNullable(callLocation);
	}

	public long getSequence() {
		return sequence;
	}

	public Level getLevel() {
		return level;
	}

	public String getMessage() {
		return message;
	}

//	Logger getLogger() {
//		return logger;
//	}

	public String getLoggerName() {
		return loggerName;
	}

	public Instant getInstant() {
		return instant;
	}

	public Optional<Throwable> getThrowable() {
		return throwable;
	}

	public Optional<StackTraceElement> getCallLocation() {
		return callLocation;
	}

	@Override
	public int hashCode() {
		return (int) sequence;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ThinLogEntry))
			return false;
		return sequence == ((ThinLogEntry) obj).sequence;
	}

	@Override
	public String toString() {
		return message;
	}

}
