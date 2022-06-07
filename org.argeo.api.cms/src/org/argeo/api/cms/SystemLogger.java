package org.argeo.api.cms;

import java.lang.System.Logger;
import java.util.Objects;
import java.util.function.Supplier;

/** Workaround because Android does not support {@link Logger}. */
abstract class SystemLogger {
	public enum Level {
		ALL(Integer.MIN_VALUE, java.lang.System.Logger.Level.ALL), //
		TRACE(400, java.lang.System.Logger.Level.TRACE), //
		DEBUG(500, java.lang.System.Logger.Level.DEBUG), //
		INFO(800, java.lang.System.Logger.Level.INFO), //
		WARNING(900, java.lang.System.Logger.Level.WARNING), //
		ERROR(1000, java.lang.System.Logger.Level.ERROR), //
		OFF(Integer.MAX_VALUE, java.lang.System.Logger.Level.OFF); //

		private final int severity;
		private java.lang.System.Logger.Level systemLevel;

		private Level(int severity, java.lang.System.Logger.Level systemLevel) {
			this.severity = severity;
			this.systemLevel = systemLevel;
		}

		public final int getSeverity() {
			return severity;
		}

		public java.lang.System.Logger.Level getSystemLevel() {
			return systemLevel;
		}
	}

	public boolean isLoggable(Level level) {
		return false;
	}

	public abstract void log(Level level, Supplier<String> msgSupplier, Throwable thrown);

	public abstract void log(Level level, String msg, Throwable thrown);

	public void log(Level level, String msg) {
		log(level, msg, (Throwable) null);
	}

	public void log(Level level, Supplier<String> msgSupplier) {
		log(level, msgSupplier, (Throwable) null);
	}

	public void log(Level level, Object obj) {
		Objects.requireNonNull(obj);
		log(level, obj.toString());
	}

	public void log(Level level, String format, Object... params) {
		// FIXME implement it
		String msg = null;
		log(level, msg);
	}

}

/** A trivial implementation wrapping a platform logger. */
class LoggerWrapper implements CmsLog {
	private final SystemLogger logger;

	LoggerWrapper(SystemLogger logger) {
		this.logger = logger;
	}

	@Override
	public SystemLogger getLogger() {
		return logger;
	}

}

class RealSystemLogger extends SystemLogger {
	final Logger logger;

	RealSystemLogger(String name) {
		logger = System.getLogger(name);
	}

	@Override
	public boolean isLoggable(Level level) {
		return logger.isLoggable(convertSystemLevel(level));
	}

	@Override
	public void log(Level level, Supplier<String> msgSupplier, Throwable thrown) {
		logger.log(convertSystemLevel(level), msgSupplier, thrown);
	}

	@Override
	public void log(Level level, String msg, Throwable thrown) {
		logger.log(convertSystemLevel(level), msg, thrown);
	}

	System.Logger.Level convertSystemLevel(Level level) {
		return level.getSystemLevel();
	}
};

class FallBackSystemLogger extends SystemLogger {
	@Override
	public void log(Level level, Supplier<String> msgSupplier, Throwable thrown) {
		if (isLoggable(level))
			if (thrown != null) {
				System.err.println(msgSupplier.get());
				thrown.printStackTrace();
			} else {
				System.out.println(msgSupplier.get());
			}
	}

	@Override
	public void log(Level level, String msg, Throwable thrown) {
		if (isLoggable(level))
			if (thrown != null) {
				System.err.println(msg);
				thrown.printStackTrace();
			} else {
				System.out.println(msg);
			}
	}
}