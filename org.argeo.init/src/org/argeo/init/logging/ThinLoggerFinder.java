package org.argeo.init.logging;

import java.lang.System.Logger;
import java.lang.System.LoggerFinder;

/** Factory for Java system logging. */
public class ThinLoggerFinder extends LoggerFinder {
	private static ThinLogging logging;

	public ThinLoggerFinder() {
		if (logging != null)
			throw new IllegalStateException("Only one logging can be initialised.");
		logging = new ThinLogging();
	}

	@Override
	public Logger getLogger(String name, Module module) {
		return logging.getLogger(name, module);
	}

	/**
	 * Falls back to java.util.logging if thin logging was not already initialised.
	 */
	public static void lazyInit() {
		if (logging != null)
			return;
		logging = new ThinLogging();
		ThinJavaUtilLogging.init();
	}

	public static Logger getLogger(String name) {
		return logging.getLogger(name, null);
	}
}
