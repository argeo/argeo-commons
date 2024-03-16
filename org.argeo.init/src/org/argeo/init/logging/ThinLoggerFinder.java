package org.argeo.init.logging;

import java.io.Serializable;
import java.lang.System.Logger;
import java.lang.System.LoggerFinder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Factory for Java system logging. As it has to be a public class in order to
 * be exposed as a service provider, it is also the main entry point for the
 * thin logging system, via static methos.
 */
public class ThinLoggerFinder extends LoggerFinder
		implements Consumer<Map<String, Object>>, Supplier<Flow.Publisher<Map<String, Serializable>>> {
	private static ThinLogging logging;
	private static ThinJavaUtilLogging javaUtilLogging;

	public ThinLoggerFinder() {
		if (logging != null)
			throw new IllegalStateException("Only one logging can be initialised.");
//		init();
	}

	@Override
	public Logger getLogger(String name, Module module) {
		lazyInit();
		Objects.requireNonNull(name);
		Logger logger = logging.getLogger(name, module);
		Objects.requireNonNull(logger);
		return logger;
	}

	private static void init() {
		logging = new ThinLogging();
		reloadConfiguration();
	}

	/** Reload configuration form system properties */
	public static void reloadConfiguration() {
		if (logging == null)
			return;
		Map<String, Object> configuration = new HashMap<>();
		for (Object key : System.getProperties().keySet()) {
			Objects.requireNonNull(key);
			String property = key.toString();
			if (property.startsWith(ThinLogging.LEVEL_PROPERTY_PREFIX)
					|| property.equals(ThinLogging.DEFAULT_LEVEL_PROPERTY))
				configuration.put(property, System.getProperty(property));
		}
		logging.accept(configuration);
	}

	/**
	 * Falls back to java.util.logging if thin logging was not already initialised
	 * by the {@link LoggerFinder} mechanism.
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

	static Consumer<Map<String, Object>> getConfigurationConsumer() {
		Objects.requireNonNull(logging);
		return logging;
	}

	static Flow.Publisher<Map<String, Serializable>> getLogEntryPublisher() {
		Objects.requireNonNull(logging);
		return logging.getLogEntryPublisher();
	}

	@Override
	public Publisher<Map<String, Serializable>> get() {
		return getLogEntryPublisher();
	}

	static void update(Map<String, Object> configuration) {
		if (logging == null)
			throw new IllegalStateException("Thin logging must be initialized first");
		logging.accept(configuration);
		if (javaUtilLogging != null)
			javaUtilLogging.readConfiguration(logging.getLevels());
	}

	static Logger getLogger(String name) {
		return logging.getLogger(name, null);
	}

	@Override
	public void accept(Map<String, Object> t) {
		if (logging != null) {
			// delegate to thin logging
			logging.accept(t);
		} else {
			// ignore
			// TODO try to congure Java logging ?
		}

	}

}
