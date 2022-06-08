package org.argeo.api.cms;

import java.lang.System.Logger;
import java.text.MessageFormat;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A Commons Logging / SLF4J style logging utilities usually wrapping a standard
 * Java platform {@link Logger}, but which can fallback to other mechanism, if a
 * system logger is not available.
 */
public interface CmsLog {
	/*
	 * SYSTEM LOGGER STYLE METHODS
	 */
	boolean isLoggable(Level level);

	void log(Level level, Supplier<String> msgSupplier, Throwable thrown);

	void log(Level level, String msg, Throwable thrown);

	void log(Level level, String format, Object... params);

	default void log(Level level, String msg) {
		log(level, msg, (Throwable) null);
	}

	default void log(Level level, Supplier<String> msgSupplier) {
		log(level, msgSupplier, (Throwable) null);
	}

	default void log(Level level, Object obj) {
		Objects.requireNonNull(obj);
		log(level, obj.toString());
	}

	/*
	 * SLF4j / COMMONS LOGGING STYLE METHODS
	 */
	@Deprecated
	CmsLog getLogger();

	default boolean isDebugEnabled() {
		return getLogger().isLoggable(Level.DEBUG);
	}

	default boolean isErrorEnabled() {
		return getLogger().isLoggable(Level.ERROR);
	}

	default boolean isInfoEnabled() {
		return getLogger().isLoggable(Level.INFO);
	}

	default boolean isTraceEnabled() {
		return getLogger().isLoggable(Level.TRACE);
	}

	default boolean isWarnEnabled() {
		return getLogger().isLoggable(Level.WARNING);
	}

	/*
	 * TRACE
	 */

	default void trace(String message) {
		getLogger().log(Level.TRACE, message);
	}

	default void trace(Supplier<String> message) {
		getLogger().log(Level.TRACE, message);
	}

	default void trace(Object message) {
		getLogger().log(Level.TRACE, Objects.requireNonNull(message));
	}

	default void trace(String message, Throwable t) {
		getLogger().log(Level.TRACE, message, t);
	}

	default void trace(Object message, Throwable t) {
		trace(Objects.requireNonNull(message).toString(), t);
	}

	default void trace(String format, Object... arguments) {
		getLogger().log(Level.TRACE, format, arguments);
	}

	/*
	 * DEBUG
	 */

	default void debug(String message) {
		getLogger().log(Level.DEBUG, message);
	}

	default void debug(Supplier<String> message) {
		getLogger().log(Level.DEBUG, message);
	}

	default void debug(Object message) {
		getLogger().log(Level.DEBUG, message);
	}

	default void debug(String message, Throwable t) {
		getLogger().log(Level.DEBUG, message, t);
	}

	default void debug(Object message, Throwable t) {
		debug(Objects.requireNonNull(message).toString(), t);
	}

	default void debug(String format, Object... arguments) {
		getLogger().log(Level.DEBUG, format, arguments);
	}

	/*
	 * INFO
	 */

	default void info(String message) {
		getLogger().log(Level.INFO, message);
	}

	default void info(Supplier<String> message) {
		getLogger().log(Level.INFO, message);
	}

	default void info(Object message) {
		getLogger().log(Level.INFO, message);
	}

	default void info(String message, Throwable t) {
		getLogger().log(Level.INFO, message, t);
	}

	default void info(Object message, Throwable t) {
		info(Objects.requireNonNull(message).toString(), t);
	}

	default void info(String format, Object... arguments) {
		getLogger().log(Level.INFO, format, arguments);
	}

	/*
	 * WARN
	 */

	default void warn(String message) {
		getLogger().log(Level.WARNING, message);
	}

	default void warn(Supplier<String> message) {
		getLogger().log(Level.WARNING, message);
	}

	default void warn(Object message) {
		getLogger().log(Level.WARNING, message);
	}

	default void warn(String message, Throwable t) {
		getLogger().log(Level.WARNING, message, t);
	}

	default void warn(Object message, Throwable t) {
		warn(Objects.requireNonNull(message).toString(), t);
	}

	default void warn(String format, Object... arguments) {
		getLogger().log(Level.WARNING, format, arguments);
	}

	/*
	 * ERROR
	 */

	default void error(String message) {
		getLogger().log(Level.ERROR, message);
	}

	default void error(Supplier<String> message) {
		getLogger().log(Level.ERROR, message);
	}

	default void error(Object message) {
		getLogger().log(Level.ERROR, message);
	}

	default void error(String message, Throwable t) {
		getLogger().log(Level.ERROR, message, t);
	}

	default void error(Object message, Throwable t) {
		error(Objects.requireNonNull(message).toString(), t);
	}

	default void error(String format, Object... arguments) {
		getLogger().log(Level.ERROR, format, arguments);
	}

	/**
	 * Exact mapping of ${java.lang.System.Logger.Level}, in case it is not
	 * available.
	 */
	public static enum Level {
		ALL(Integer.MIN_VALUE), //
		TRACE(400), //
		DEBUG(500), //
		INFO(800), //
		WARNING(900), //
		ERROR(1000), //
		OFF(Integer.MAX_VALUE); //

		final int severity;

		private Level(int severity) {
			this.severity = severity;
		}

		public final int getSeverity() {
			return severity;
		}
	}

	/*
	 * STATIC UTILITIES
	 */

	static CmsLog getLog(Class<?> clss) {
		return getLog(Objects.requireNonNull(clss).getName());
	}

	static CmsLog getLog(String name) {
		if (isSystemLoggerAvailable) {
			return new SystemCmsLog(name);
		} else { // typically Android
			return new FallBackCmsLog();
		}
	}

	static final boolean isSystemLoggerAvailable = isSystemLoggerAvailable();

	static boolean isSystemLoggerAvailable() {
		try {
			Logger logger = System.getLogger(CmsLog.class.getName());
			logger.log(java.lang.System.Logger.Level.TRACE, () -> "System logger is available.");
			return true;
		} catch (NoSuchMethodError | NoClassDefFoundError e) {// Android
			return false;
		}
	}
}

/**
 * Uses {@link System.Logger}, should be used on proper implementations of the
 * Java platform.
 */
class SystemCmsLog implements CmsLog {
	private final Logger logger;

	SystemCmsLog(String name) {
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

	java.lang.System.Logger.Level convertSystemLevel(Level level) {
		switch (level.severity) {
		case Integer.MIN_VALUE:
			return java.lang.System.Logger.Level.ALL;
		case 400:
			return java.lang.System.Logger.Level.TRACE;
		case 500:
			return java.lang.System.Logger.Level.DEBUG;
		case 800:
			return java.lang.System.Logger.Level.INFO;
		case 900:
			return java.lang.System.Logger.Level.WARNING;
		case 1000:
			return java.lang.System.Logger.Level.ERROR;
		case Integer.MAX_VALUE:
			return java.lang.System.Logger.Level.OFF;
		default:
			throw new IllegalArgumentException("Unexpected value: " + level.severity);
		}
	}

	@Override
	public void log(Level level, String format, Object... params) {
		logger.log(convertSystemLevel(level), format, params);
	}

	@Override
	public CmsLog getLogger() {
		return this;
	}
};

/** Dummy fallback for non-standard platforms such as Android. */
class FallBackCmsLog implements CmsLog {
	@Override
	public boolean isLoggable(Level level) {
		return level.getSeverity() >= 800;// INFO and higher
	}

	@Override
	public void log(Level level, Supplier<String> msgSupplier, Throwable thrown) {
		if (isLoggable(level))
			if (thrown != null || level.getSeverity() >= 900) {
				System.err.println(msgSupplier.get());
				thrown.printStackTrace();
			} else {
				System.out.println(msgSupplier.get());
			}
	}

	@Override
	public void log(Level level, String msg, Throwable thrown) {
		if (isLoggable(level))
			if (thrown != null || level.getSeverity() >= 900) {
				System.err.println(msg);
				thrown.printStackTrace();
			} else {
				System.out.println(msg);
			}
	}

	@Override
	public void log(Level level, String format, Object... params) {
		if (format == null)
			return;
		String msg = MessageFormat.format(format, params);
		log(level, msg);
	}

	@Override
	public CmsLog getLogger() {
		return this;
	}
}
