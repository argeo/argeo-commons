package org.slf4j.impl;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A Commons Logging / SLF4J style logging utilities wrapping a standard Java
 * platform {@link Logger}.
 */
public interface SystemLoggingAdapter {
	Logger getLogger();

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

	/*
	 * STATIC UTILITIES
	 */

	static SystemLoggingAdapter getLog(Class<?> clss) {
		return getLog(Objects.requireNonNull(clss).getName());
	}

	static SystemLoggingAdapter getLog(String name) {
		Logger logger = System.getLogger(Objects.requireNonNull(name));
		return new LoggerWrapper(logger);
	}

	/** A trivial implementation wrapping a platform logger. */
	static class LoggerWrapper implements SystemLoggingAdapter {
		private final Logger logger;

		LoggerWrapper(Logger logger) {
			this.logger = logger;
		}

		@Override
		public Logger getLogger() {
			return logger;
		}

	}

}
