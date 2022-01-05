package org.apache.commons.logging;

import java.lang.System.Logger;
import java.util.Objects;

/** A pseudo implementation of Apache Commons Logging. */
@Deprecated
public abstract class LogFactory {
	public static Log getLog(Class<?> clss) {
		return getLog(Objects.requireNonNull(clss).getName());
	}

	public static Log getLog(String name) {
		Logger logger = System.getLogger(Objects.requireNonNull(name));
		return new LoggerWrapper(logger);
	}

	static class LoggerWrapper implements Log {
		private final Logger logger;

		LoggerWrapper(Logger logger) {
			super();
			this.logger = logger;
		}

		@Override
		public Logger getLogger() {
			return logger;
		}

	}
}
