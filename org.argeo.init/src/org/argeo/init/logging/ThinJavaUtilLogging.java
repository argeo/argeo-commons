package org.argeo.init.logging;

import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Fallback wrapper around the java.util.logging framework, when thinb logging
 * could not be instantiated directly.
 */
class ThinJavaUtilLogging {
	public static void init() {
		LogManager logManager = LogManager.getLogManager();
		logManager.reset();
		Logger rootLogger = logManager.getLogger("");
		rootLogger.addHandler(new ThinHandler());
		rootLogger.setLevel(java.util.logging.Level.FINE);
	}
}
