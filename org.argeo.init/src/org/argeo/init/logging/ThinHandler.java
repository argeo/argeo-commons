package org.argeo.init.logging;

import java.lang.System.Logger.Level;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

/**
 * A fallback {@link Handler} forwarding only messages and logger name (all
 * other {@link LogRecord} information is lost.
 */
class ThinHandler extends Handler {
	@Override
	public void publish(LogRecord record) {
		java.lang.System.Logger systemLogger = ThinLoggerFinder.getLogger(record.getLoggerName());
		systemLogger.log(fromJulLevel(record.getLevel()), record.getMessage());
	}

	protected Level fromJulLevel(java.util.logging.Level julLevel) {
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

	@Override
	public void flush() {
	}

	@Override
	public void close() throws SecurityException {
	}

}