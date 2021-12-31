package org.argeo.init.logging;

import java.lang.System.Logger;
import java.lang.System.LoggerFinder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Factory for Java system logging. As it has to be a public class in order to
 * be exposed as a service provider, it is also the main entry point for the
 * thin logging system, via static methos.
 */
public class ThinLoggerFinder extends LoggerFinder {
	private static ThinLogging logging;
	private static ThinJavaUtilLogging javaUtilLogging;

	public ThinLoggerFinder() {
		if (logging != null)
			throw new IllegalStateException("Only one logging can be initialised.");
		init();
	}

	@Override
	public Logger getLogger(String name, Module module) {
		return logging.getLogger(name, module);
	}

	private static void init() {
		logging = new ThinLogging();

		Map<String, String> configuration = new HashMap<>();
		for (Object key : System.getProperties().keySet()) {
			Objects.requireNonNull(key);
			String property = key.toString();
			if (property.startsWith(ThinLogging.LEVEL_PROPERTY_PREFIX)
					|| property.equals(ThinLogging.DEFAULT_LEVEL_PROPERTY))
				configuration.put(property, System.getProperty(property));
		}
		logging.update(configuration);
	}

	/**
	 * Falls back to java.util.logging if thin logging was not already initialised.
	 */
	public static void lazyInit() {
		if (logging != null)
			return;
		if (javaUtilLogging != null)
			return;
		init();
		javaUtilLogging = ThinJavaUtilLogging.init();
		javaUtilLogging.readConfiguration(logging.getLevels());
	}

	public static void update(Map<String, String> configuration) {
		if (logging == null)
			throw new IllegalStateException("Thin logging must be initialized first");
		logging.update(configuration);
		if (javaUtilLogging != null)
			javaUtilLogging.readConfiguration(logging.getLevels());
	}

	static Logger getLogger(String name) {
		return logging.getLogger(name, null);
	}
}
