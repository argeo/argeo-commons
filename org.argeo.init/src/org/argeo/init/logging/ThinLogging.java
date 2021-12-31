package org.argeo.init.logging;

import java.io.PrintStream;
import java.io.Serializable;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * A thin logging system based on the {@link Logger} framework. It is a
 * {@link Consumer} of configuration, and can be registered as such.
 */
class ThinLogging implements Consumer<Map<String, Object>> {
	final static String DEFAULT_LEVEL_NAME = "";

	final static String DEFAULT_LEVEL_PROPERTY = "log";
	final static String LEVEL_PROPERTY_PREFIX = DEFAULT_LEVEL_PROPERTY + ".";

	final static String JOURNALD_PROPERTY = "argeo.logging.journald";
	final static String CALL_LOCATION_PROPERTY = "argeo.logging.callLocation";

	private final static AtomicLong nextEntry = new AtomicLong(0l);

	// we don't synchronize maps on purpose as it would be
	// too expensive during normal operation
	// updates to the config may be shortly inconsistent
	private SortedMap<String, ThinLogger> loggers = new TreeMap<>();
	private NavigableMap<String, Level> levels = new TreeMap<>();
	private volatile boolean updatingConfiguration = false;

	private final ExecutorService executor;
	private final LogEntryPublisher publisher;

	private final boolean journald;
	private final Level callLocationLevel;

	ThinLogging() {
		executor = Executors.newCachedThreadPool((r) -> {
			Thread t = new Thread(r);
			t.setDaemon(true);
			return t;
		});
		publisher = new LogEntryPublisher(executor, Flow.defaultBufferSize());

		PrintStreamSubscriber subscriber = new PrintStreamSubscriber();
		publisher.subscribe(subscriber);

		Runtime.getRuntime().addShutdownHook(new Thread(() -> close(), "Log shutdown"));

		// initial default level
		levels.put("", Level.WARNING);

		// Logging system config
		// journald

//		Map<String, String> env = new TreeMap<>(System.getenv());
//		for (String key : env.keySet()) {
//			System.out.println(key + "=" + env.get(key));
//		}

		String journaldStr = System.getProperty(JOURNALD_PROPERTY, "auto");
		switch (journaldStr) {
		case "auto":
			String systemdInvocationId = System.getenv("INVOCATION_ID");
			if (systemdInvocationId != null) {// in systemd
				// check whether we are indirectly in a desktop app (e.g. eclipse)
				String desktopFilePid = System.getenv("GIO_LAUNCHED_DESKTOP_FILE_PID");
				if (desktopFilePid != null) {
					Long javaPid = ProcessHandle.current().pid();
					if (!javaPid.toString().equals(desktopFilePid)) {
						journald = false;
						break;
					}
				}
				journald = true;
				break;
			}
			journald = false;
			break;
		case "true":
		case "on":
			journald = true;
			break;
		case "false":
		case "off":
			journald = false;
			break;
		default:
			throw new IllegalArgumentException(
					"Unsupported value '" + journaldStr + "' for property " + JOURNALD_PROPERTY);
		}

		String callLocationStr = System.getProperty(CALL_LOCATION_PROPERTY, Level.WARNING.getName());
		callLocationLevel = Level.valueOf(callLocationStr);
	}

	private void close() {
		publisher.close();
		try {
			// we ait a bit in order to make sure all messages are flushed
			// TODO synchronize more efficiently
			executor.awaitTermination(300, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// silent
		}
	}

	private Level computeApplicableLevel(String name) {
		Map.Entry<String, Level> entry = levels.floorEntry(name);
		assert entry != null;
		return entry.getValue();

	}

//	private boolean isLoggable(String name, Level level) {
//		Objects.requireNonNull(name);
//		Objects.requireNonNull(level);
//
//		if (updatingConfiguration) {
//			synchronized (levels) {
//				try {
//					levels.wait();
//					// TODO make exit more robust
//				} catch (InterruptedException e) {
//					throw new IllegalStateException(e);
//				}
//			}
//		}
//
//		return level.getSeverity() >= computeApplicableLevel(name).getSeverity();
//	}

	public Logger getLogger(String name, Module module) {
		if (!loggers.containsKey(name)) {
			ThinLogger logger = new ThinLogger(name, computeApplicableLevel(name));
			loggers.put(name, logger);
		}
		return loggers.get(name);
	}

	public void accept(Map<String, Object> configuration) {
		synchronized (levels) {
			updatingConfiguration = true;

			Map<String, Level> backup = new TreeMap<>(levels);

			boolean fullReset = configuration.containsKey(DEFAULT_LEVEL_PROPERTY);
			try {
				properties: for (String property : configuration.keySet()) {
					if (!property.startsWith(LEVEL_PROPERTY_PREFIX))
						continue properties;
					String levelStr = configuration.get(property).toString();
					Level level = Level.valueOf(levelStr);
					levels.put(property.substring(LEVEL_PROPERTY_PREFIX.length()), level);
				}

				if (fullReset) {
					Iterator<Map.Entry<String, Level>> it = levels.entrySet().iterator();
					while (it.hasNext()) {
						Map.Entry<String, Level> entry = it.next();
						String name = entry.getKey();
						if (!configuration.containsKey(LEVEL_PROPERTY_PREFIX + name)) {
							it.remove();
						}
					}
					Level newDefaultLevel = Level.valueOf(configuration.get(DEFAULT_LEVEL_PROPERTY).toString());
					levels.put(DEFAULT_LEVEL_NAME, newDefaultLevel);
					// TODO notify everyone?
				}
				assert levels.containsKey(DEFAULT_LEVEL_NAME);

				// recompute all levels
				for (String name : loggers.keySet()) {
					ThinLogger logger = loggers.get(name);
					logger.setLevel(computeApplicableLevel(name));
				}
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				levels.clear();
				levels.putAll(backup);
			}
			updatingConfiguration = false;
			levels.notifyAll();
		}

	}

	Flow.Publisher<Map<String, Serializable>> getLogEntryPublisher() {
		return publisher;
	}

	Map<String, Level> getLevels() {
		return Collections.unmodifiableNavigableMap(levels);
	}

	/*
	 * INTERNAL CLASSES
	 */
	
	private class ThinLogger implements System.Logger {
		private final String name;

		private Level level;

		protected ThinLogger(String name, Level level) {
			assert Objects.nonNull(name);
			this.name = name;
			this.level = level;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean isLoggable(Level level) {
			return level.getSeverity() >= getLevel().getSeverity();
			// TODO optimise by referencing the applicable level in this class?
//			return ThinLogging.this.isLoggable(name, level);
		}

		private Level getLevel() {
			if (updatingConfiguration) {
				synchronized (levels) {
					try {
						levels.wait();
						// TODO make exit more robust
					} catch (InterruptedException e) {
						throw new IllegalStateException(e);
					}
				}
			}
			return level;
		}

		@Override
		public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
			if (!isLoggable(level))
				return;
			// measure timestamp first
			Instant now = Instant.now();
			Thread thread = Thread.currentThread();
			publisher.log(this, level, bundle, msg, now, thread, thrown, findCallLocation(level, thread));
		}

		@Override
		public void log(Level level, ResourceBundle bundle, String format, Object... params) {
			if (!isLoggable(level))
				return;
			// measure timestamp first
			Instant now = Instant.now();
			Thread thread = Thread.currentThread();

			// NOTE: this is the method called when logging a plain message without
			// exception, so it should be considered as a format only when args are not null
			String msg = params == null ? format : MessageFormat.format(format, params);
			publisher.log(this, level, bundle, msg, now, thread, (Throwable) null, findCallLocation(level, thread));
		}

		private void setLevel(Level level) {
			this.level = level;
		}

		private StackTraceElement findCallLocation(Level level, Thread thread) {
			assert level != null;
			assert thread != null;
			StackTraceElement callLocation = null;
			if (level.getSeverity() >= callLocationLevel.getSeverity()) {
				StackTraceElement[] stack = thread.getStackTrace();
				int lowestLoggerInterface = 0;
				stack: for (int i = 2; i < stack.length; i++) {
					String className = stack[i].getClassName();
					switch (className) {
					// TODO make it more configurable
					case "java.lang.System$Logger":
					case "java.util.logging.Logger":
					case "org.apache.commons.logging.Log":
					case "org.osgi.service.log.Logger":
					case "org.argeo.cms.Log":
					case "org.slf4j.impl.ArgeoLogger":
					case "org.eclipse.jetty.util.log.Slf4jLog":
						lowestLoggerInterface = i;
						continue stack;
					default:
					}
				}
				if (stack.length > lowestLoggerInterface + 1)
					callLocation = stack[lowestLoggerInterface + 1];
			}
			return callLocation;
		}

	}

	private final static String KEY_LOGGER = Logger.class.getName();
	private final static String KEY_LEVEL = Level.class.getName();
	private final static String KEY_MSG = String.class.getName();
	private final static String KEY_THROWABLE = Throwable.class.getName();
	private final static String KEY_INSTANT = Instant.class.getName();
	private final static String KEY_CALL_LOCATION = StackTraceElement.class.getName();
	private final static String KEY_THREAD = Thread.class.getName();

	private class LogEntryPublisher extends SubmissionPublisher<Map<String, Serializable>> {

		private LogEntryPublisher(Executor executor, int maxBufferCapacity) {
			super(executor, maxBufferCapacity);
		}

		private void log(ThinLogger logger, Level level, ResourceBundle bundle, String msg, Instant instant,
				Thread thread, Throwable thrown, StackTraceElement callLocation) {
			assert level != null;
			assert logger != null;
			assert msg != null;
			assert instant != null;
			assert thread != null;

			final long sequence = nextEntry.incrementAndGet();

			Map<String, Serializable> logEntry = new LogEntryMap(sequence);

			// same object as key class name
			logEntry.put(KEY_LEVEL, level);
			logEntry.put(KEY_MSG, msg);
			logEntry.put(KEY_INSTANT, instant);
			if (thrown != null)
				logEntry.put(KEY_THROWABLE, thrown);
			if (callLocation != null)
				logEntry.put(KEY_CALL_LOCATION, callLocation);

			// object is a string
			logEntry.put(KEY_LOGGER, logger.getName());
			logEntry.put(KEY_THREAD, thread.getName());

			// should be unmodifiable for security reasons
			submit(Collections.unmodifiableMap(logEntry));
		}

	}

	/**
	 * An internal optimisation for collections. It should not be referred to
	 * directly as a type.
	 */
	// TODO optimise memory with a custom map implementation?
	// but access may be slower
	private static class LogEntryMap extends HashMap<String, Serializable> {
		private static final long serialVersionUID = 7361434381922521356L;

		private final long sequence;

		private LogEntryMap(long sequence) {
			// maximum 7 fields, so using default load factor 0.75
			// an initial size of 10 should prevent rehashing (7 / 0.75 ~ 9.333)
			// see HashMap class description for more details
			super(10, 0.75f);
			this.sequence = sequence;
		}

		@Override
		public boolean equals(Object o) {
			if (o instanceof LogEntryMap)
				return sequence == ((LogEntryMap) o).sequence;
			else if (o instanceof Map) {
				Map<?, ?> map = (Map<?, ?>) o;
				return get(KEY_INSTANT).equals(map.get(KEY_INSTANT)) && get(KEY_THREAD).equals(map.get(KEY_THREAD));
			} else
				return false;
		}

		@Override
		public int hashCode() {
			return (int) sequence;
		}

	}

	private class PrintStreamSubscriber implements Flow.Subscriber<Map<String, Serializable>> {
		private PrintStream out;
		private PrintStream err;
		private int writeToErrLevel = Level.WARNING.getSeverity();

		protected PrintStreamSubscriber() {
			this(System.out, System.err);
		}

		protected PrintStreamSubscriber(PrintStream out, PrintStream err) {
			this.out = out;
			this.err = err;
		}

		private Level getLevel(Map<String, Serializable> logEntry) {
			return (Level) logEntry.get(KEY_LEVEL);
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(Map<String, Serializable> item) {
			if (getLevel(item).getSeverity() >= writeToErrLevel) {
				err.print(toPrint(item));
			} else {
				out.print(toPrint(item));
			}
			// TODO flush for journald?
		}

		@Override
		public void onError(Throwable throwable) {
			throwable.printStackTrace(err);
		}

		@Override
		public void onComplete() {
			out.flush();
			err.flush();
		}

		protected String firstLinePrefix(Map<String, Serializable> logEntry) {
			Level level = getLevel(logEntry);
			String spaces;
			switch (level) {
			case ERROR:
			case DEBUG:
			case TRACE:
				spaces = "   ";
				break;
			case INFO:
				spaces = "    ";
				break;
			case WARNING:
				spaces = " ";
				break;
			case ALL:
				spaces = "     ";
				break;
			default:
				throw new IllegalArgumentException("Unsupported level " + level);
			}
			return journald ? linePrefix(logEntry) : logEntry.get(KEY_INSTANT) + " " + level + spaces;
		}

		protected String firstLineSuffix(Map<String, Serializable> logEntry) {
			return " - " + (logEntry.containsKey(KEY_CALL_LOCATION) ? logEntry.get(KEY_CALL_LOCATION)
					: logEntry.get(KEY_LOGGER)) + " [" + logEntry.get(KEY_THREAD) + "]";
		}

		protected String linePrefix(Map<String, Serializable> logEntry) {
			return journald ? "<" + levelToJournald(getLevel(logEntry)) + ">" : "";
		}

		protected int levelToJournald(Level level) {
			int severity = level.getSeverity();
			if (severity >= Level.ERROR.getSeverity())
				return 3;
			else if (severity >= Level.WARNING.getSeverity())
				return 4;
			else if (severity >= Level.INFO.getSeverity())
				return 6;
			else
				return 7;
		}

		protected String toPrint(Map<String, Serializable> logEntry) {
			StringBuilder sb = new StringBuilder();
			StringTokenizer st = new StringTokenizer((String) logEntry.get(KEY_MSG), "\r\n");
			assert st.hasMoreTokens();

			// first line
			String firstLine = st.nextToken();
			sb.append(firstLinePrefix(logEntry));
			sb.append(firstLine);
			sb.append(firstLineSuffix(logEntry));
			sb.append('\n');

			// other lines
			String prefix = linePrefix(logEntry);
			while (st.hasMoreTokens()) {
				sb.append(prefix);
				sb.append(st.nextToken());
				sb.append('\n');
			}

			if (logEntry.containsKey(KEY_THROWABLE)) {
				Throwable throwable = (Throwable) logEntry.get(KEY_THROWABLE);
				sb.append(prefix);
				addThrowable(sb, prefix, throwable);
			}
			return sb.toString();
		}

		protected void addThrowable(StringBuilder sb, String prefix, Throwable throwable) {
			sb.append(throwable.getClass().getName());
			sb.append(": ");
			sb.append(throwable.getMessage());
			sb.append('\n');
			for (StackTraceElement ste : throwable.getStackTrace()) {
				sb.append(prefix);
				sb.append(ste.toString());
				sb.append('\n');
			}
			if (throwable.getCause() != null) {
				sb.append(prefix);
				sb.append("caused by ");
				addThrowable(sb, prefix, throwable.getCause());
			}
		}
	}

	public static void main(String args[]) {
		Logger logger = System.getLogger(ThinLogging.class.getName());
		logger.log(Logger.Level.ALL, "Log all");
		logger.log(Logger.Level.TRACE, "Multi\nline\ntrace");
		logger.log(Logger.Level.DEBUG, "Log debug");
		logger.log(Logger.Level.INFO, "Log info");
		logger.log(Logger.Level.WARNING, "Log warning");
		logger.log(Logger.Level.ERROR, "Log exception", new Throwable());
	}

}
