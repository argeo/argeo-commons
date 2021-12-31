package org.argeo.init.logging;

import java.io.PrintStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.Collections;
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

/** A thin logging system based on the {@link Logger} framework. */
class ThinLogging {
	final static String DEFAULT_LEVEL_NAME = "";

	final static String DEFAULT_LEVEL_PROPERTY = "log";
	final static String LEVEL_PROPERTY_PREFIX = DEFAULT_LEVEL_PROPERTY + ".";

	// we don't synchronize maps on purpose as it would be
	// too expensive during normal operation
	// updates to the config may be shortly inconsistent
	private SortedMap<String, ThinLogger> loggers = new TreeMap<>();
	private NavigableMap<String, Level> levels = new TreeMap<>();
	private volatile boolean updatingConfiguration = false;

	private final ExecutorService executor;
	private final LogEntryPublisher publisher;

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
	}

	protected void close() {
		publisher.close();
		try {
			// we ait a bit in order to make sure all messages are flushed
			// TODO synchronize more efficiently
			executor.awaitTermination(300, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			// silent
		}
	}

	public boolean isLoggable(String name, Level level) {
		Objects.requireNonNull(name);
		Objects.requireNonNull(level);

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

		Map.Entry<String, Level> entry = levels.ceilingEntry(name);
		assert entry != null;
		return level.getSeverity() >= entry.getValue().getSeverity();
	}

	public Logger getLogger(String name, Module module) {
		if (!loggers.containsKey(name)) {
			ThinLogger logger = new ThinLogger(name);
			loggers.put(name, logger);
		}
		return loggers.get(name);
	}

	void update(Map<String, String> configuration) {
		synchronized (levels) {
			updatingConfiguration = true;

			Map<String, Level> backup = new TreeMap<>(levels);

			boolean fullReset = configuration.containsKey(DEFAULT_LEVEL_PROPERTY);
			try {
				properties: for (String property : configuration.keySet()) {
					if (!property.startsWith(LEVEL_PROPERTY_PREFIX))
						continue properties;
					String levelStr = configuration.get(property);
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
//				for (String name : levels.keySet()) {
//					if (!configuration.containsKey(LEVEL_PROPERTY_PREFIX + name)) {
//						levels.remove(name);
//					}
//				}
					Level newDefaultLevel = Level.valueOf(configuration.get(DEFAULT_LEVEL_PROPERTY));
					levels.put(DEFAULT_LEVEL_NAME, newDefaultLevel);
					// TODO notify everyone?
				}
				assert levels.containsKey(DEFAULT_LEVEL_NAME);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
				levels.clear();
				levels.putAll(backup);
			}
			updatingConfiguration = false;
			levels.notifyAll();
		}

	}

	Map<String, Level> getLevels() {
		return Collections.unmodifiableNavigableMap(levels);
	}

	class ThinLogger implements System.Logger {
		private final String name;
		private boolean callLocationEnabled = true;

		protected ThinLogger(String name) {
			assert Objects.nonNull(name);
			this.name = name;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public boolean isLoggable(Level level) {
			// TODO optimise by referencing the applicable level in this class?
			return ThinLogging.this.isLoggable(name, level);
		}

		@Override
		public void log(Level level, ResourceBundle bundle, String msg, Throwable thrown) {
			// measure timestamp first
			Instant now = Instant.now();
			publisher.log(this, level, bundle, msg, thrown, now, findCallLocation());
		}

		@Override
		public void log(Level level, ResourceBundle bundle, String format, Object... params) {
			// measure timestamp first
			Instant now = Instant.now();

			// NOTE: this is the method called when logging a plain message without
			// exception, so it should be considered as a format only when args are not null
			String msg = params == null ? format : MessageFormat.format(format, params);
			publisher.log(this, level, bundle, msg, null, now, findCallLocation());
		}

		protected StackTraceElement findCallLocation() {
			StackTraceElement callLocation = null;
			if (callLocationEnabled) {
				StackTraceElement[] stack = Thread.currentThread().getStackTrace();
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

	class LogEntryPublisher extends SubmissionPublisher<ThinLogEntry> {

		protected LogEntryPublisher(Executor executor, int maxBufferCapacity) {
			super(executor, maxBufferCapacity);
		}

		void log(ThinLogger logger, Level level, ResourceBundle bundle, String msg, Throwable thrown, Instant instant,
				StackTraceElement callLocation) {
			ThinLogEntry logEntry = new ThinLogEntry(logger, level, msg, instant, thrown, callLocation);
			submit(logEntry);
		}

	}

	class PrintStreamSubscriber implements Flow.Subscriber<ThinLogEntry> {
		private PrintStream out;
		private PrintStream err;
		private int writeToErrLevel = Level.WARNING.getSeverity();

		private boolean journald = false;

		protected PrintStreamSubscriber() {
			this(System.out, System.err);
		}

		protected PrintStreamSubscriber(PrintStream out, PrintStream err) {
			this.out = out;
			this.err = err;
		}

		@Override
		public void onSubscribe(Subscription subscription) {
			subscription.request(Long.MAX_VALUE);
		}

		@Override
		public void onNext(ThinLogEntry item) {
			if (item.getLevel().getSeverity() >= writeToErrLevel) {
				err.print(toPrint(item));
			} else {
				out.print(toPrint(item));
			}
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

		protected boolean prefixOnEachLine() {
			return journald;
		}

		protected String firstLinePrefix(ThinLogEntry logEntry) {
			return journald ? linePrefix(logEntry)
					: logEntry.getLevel().toString() + "\t" + logEntry.getInstant() + " ";
		}

		protected String firstLineSuffix(ThinLogEntry logEntry) {
			return " - " + (logEntry.getCallLocation().isEmpty() ? logEntry.getLoggerName()
					: logEntry.getCallLocation().get());
		}

		protected String linePrefix(ThinLogEntry logEntry) {
			return journald ? "<" + levelToJournald(logEntry.getLevel()) + ">" : "";
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

		protected String toPrint(ThinLogEntry logEntry) {
			StringBuilder sb = new StringBuilder();
			StringTokenizer st = new StringTokenizer(logEntry.getMessage(), "\r\n");
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

			if (!logEntry.getThrowable().isEmpty()) {
				Throwable throwable = logEntry.getThrowable().get();
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
		logger.log(Logger.Level.INFO, "Hello log!");
		logger.log(Logger.Level.ERROR, "Hello error!");
		logger.log(Logger.Level.DEBUG, "Hello multi\nline\ndebug!");
		logger.log(Logger.Level.WARNING, "Hello exception!", new Throwable());
	}

}
