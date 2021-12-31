package org.argeo.init.logging;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.System.Logger.Level;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

/**
 * Fallback wrapper around the java.util.logging framework, when thin logging
 * could not be instantiated directly.
 */
class ThinJavaUtilLogging {
	private LogManager logManager;

	private ThinJavaUtilLogging(LogManager logManager) {
		this.logManager = logManager;
		this.logManager.reset();
	}

	static ThinJavaUtilLogging init() {
		LogManager logManager = LogManager.getLogManager();
		ThinJavaUtilLogging thinJul = new ThinJavaUtilLogging(logManager);
		return thinJul;
	}

	private static Level fromJulLevel(java.util.logging.Level julLevel) {
		if (java.util.logging.Level.ALL.equals(julLevel))
			return Level.ALL;
		else if (java.util.logging.Level.FINER.equals(julLevel))
			return Level.TRACE;
		else if (java.util.logging.Level.FINE.equals(julLevel))
			return Level.DEBUG;
		else if (java.util.logging.Level.INFO.equals(julLevel))
			return Level.INFO;
		else if (java.util.logging.Level.WARNING.equals(julLevel))
			return Level.WARNING;
		else if (java.util.logging.Level.SEVERE.equals(julLevel))
			return Level.ERROR;
		else if (java.util.logging.Level.OFF.equals(julLevel))
			return Level.OFF;
		else
			throw new IllegalArgumentException("Unsupported JUL level " + julLevel);
	}

	private static java.util.logging.Level toJulLevel(Level level) {
		if (Level.ALL.equals(level))
			return java.util.logging.Level.ALL;
		else if (Level.TRACE.equals(level))
			return java.util.logging.Level.FINER;
		else if (Level.DEBUG.equals(level))
			return java.util.logging.Level.FINE;
		else if (Level.INFO.equals(level))
			return java.util.logging.Level.INFO;
		else if (Level.WARNING.equals(level))
			return java.util.logging.Level.WARNING;
		else if (Level.ERROR.equals(level))
			return java.util.logging.Level.SEVERE;
		else if (Level.OFF.equals(level))
			return java.util.logging.Level.OFF;
		else
			throw new IllegalArgumentException("Unsupported logging level " + level);
	}

	void readConfiguration(Map<String, Level> configuration) {
		this.logManager.reset();
		Properties properties = new Properties();
		for (String name : configuration.keySet()) {
			properties.put(name + ".level", toJulLevel(configuration.get(name)).toString());
		}
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			properties.store(out, null);
			try (ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray())) {
				logManager.readConfiguration(in);
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot apply JUL configuration", e);
		}
		logManager.getLogger("").addHandler(new ThinHandler());
	}

	/**
	 * A fallback {@link Handler} forwarding only messages and logger name (all
	 * other {@link LogRecord} information is lost.
	 */
	private static class ThinHandler extends Handler {
		@Override
		public void publish(LogRecord record) {
			java.lang.System.Logger systemLogger = ThinLoggerFinder.getLogger(record.getLoggerName());
			systemLogger.log(ThinJavaUtilLogging.fromJulLevel(record.getLevel()), record.getMessage());
		}

		@Override
		public void flush() {
		}

		@Override
		public void close() throws SecurityException {
		}

	}
}
