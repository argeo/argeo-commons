/*
 * Copyright (C) 2007-2012 Argeo GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.cms.internal.kernel;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggingEvent;
import org.argeo.cms.CmsException;
import org.argeo.cms.auth.CurrentUser;
import org.argeo.node.ArgeoLogListener;
import org.argeo.node.ArgeoLogger;
import org.argeo.node.NodeConstants;
import org.argeo.osgi.useradmin.UserAdminConf;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;
import org.osgi.service.log.LogReaderService;
import org.osgi.service.log.LogService;

/** Not meant to be used directly in standard log4j config */
class NodeLogger implements ArgeoLogger, LogListener {
	/** Internal debug for development purposes. */
	private static Boolean debug = false;

	// private final static Log log = LogFactory.getLog(NodeLogger.class);

	private Boolean disabled = false;

	private String level = null;

	private Level log4jLevel = null;
	// private Layout layout;

	private Properties configuration;

	private AppenderImpl appender;

	private final List<ArgeoLogListener> everythingListeners = Collections
			.synchronizedList(new ArrayList<ArgeoLogListener>());
	private final List<ArgeoLogListener> allUsersListeners = Collections
			.synchronizedList(new ArrayList<ArgeoLogListener>());
	private final Map<String, List<ArgeoLogListener>> userListeners = Collections
			.synchronizedMap(new HashMap<String, List<ArgeoLogListener>>());

	private BlockingQueue<LogEvent> events;
	private LogDispatcherThread logDispatcherThread = new LogDispatcherThread();

	private Integer maxLastEventsCount = 10 * 1000;

	/** Marker to prevent stack overflow */
	private ThreadLocal<Boolean> dispatching = new ThreadLocal<Boolean>() {

		@Override
		protected Boolean initialValue() {
			return false;
		}
	};

	public NodeLogger(LogReaderService lrs) {
		Enumeration<LogEntry> logEntries = lrs.getLog();
		while (logEntries.hasMoreElements())
			logged(logEntries.nextElement());
		lrs.addLogListener(this);

		// configure log4j watcher
		String log4jConfiguration = KernelUtils.getFrameworkProp("log4j.configuration");
		if (log4jConfiguration != null && log4jConfiguration.startsWith("file:")) {
			if (log4jConfiguration.contains("..")) {
				if (log4jConfiguration.startsWith("file://"))
					log4jConfiguration = log4jConfiguration.substring("file://".length());
				else if (log4jConfiguration.startsWith("file:"))
					log4jConfiguration = log4jConfiguration.substring("file:".length());
			}
			try {
				Path log4jconfigPath;
				if (log4jConfiguration.startsWith("file:"))
					log4jconfigPath = Paths.get(new URI(log4jConfiguration));
				else
					log4jconfigPath = Paths.get(log4jConfiguration);
				Thread log4jConfWatcher = new Log4jConfWatcherThread(log4jconfigPath);
				log4jConfWatcher.start();
			} catch (Exception e) {
				stdErr("Badly formatted log4j configuration URI " + log4jConfiguration + ": " + e.getMessage());
			}
		}
	}

	public void init() {
		try {
			events = new LinkedBlockingQueue<LogEvent>();

			// if (layout != null)
			// setLayout(layout);
			// else
			// setLayout(new PatternLayout(pattern));
			appender = new AppenderImpl();
			reloadConfiguration();
			Logger.getRootLogger().addAppender(appender);

			logDispatcherThread = new LogDispatcherThread();
			logDispatcherThread.start();
		} catch (Exception e) {
			throw new CmsException("Cannot initialize log4j");
		}
	}

	public void destroy() throws Exception {
		Logger.getRootLogger().removeAppender(appender);
		allUsersListeners.clear();
		for (List<ArgeoLogListener> lst : userListeners.values())
			lst.clear();
		userListeners.clear();

		events.clear();
		events = null;
		logDispatcherThread.interrupt();
	}

	// public void setLayout(Layout layout) {
	// this.layout = layout;
	// }

	public String toString() {
		return "Node Logger";
	}

	//
	// OSGi LOGGER
	//
	@SuppressWarnings("deprecation")
	@Override
	public void logged(LogEntry status) {
		Log pluginLog = LogFactory.getLog(status.getBundle().getSymbolicName());
		Integer severity = status.getLevel();
		if (severity == LogService.LOG_ERROR) {
			// FIXME Fix Argeo TP
			if (status.getException() instanceof SignatureException)
				return;
			// pluginLog.error(msg(status), status.getException());
			pluginLog.error(msg(status) + ": " + status.getException());
		} else if (severity == LogService.LOG_WARNING)
			pluginLog.warn(msg(status), status.getException());
		else if (severity == LogService.LOG_INFO && pluginLog.isDebugEnabled())
			pluginLog.debug(msg(status), status.getException());
		else if (severity == LogService.LOG_DEBUG && pluginLog.isTraceEnabled())
			pluginLog.trace(msg(status), status.getException());
	}

	private String msg(LogEntry status) {
		StringBuilder sb = new StringBuilder();
		sb.append(status.getMessage());
		Bundle bundle = status.getBundle();
		if (bundle != null) {
			sb.append(" '" + bundle.getSymbolicName() + "'");
		}
		ServiceReference<?> sr = status.getServiceReference();
		if (sr != null) {
			sb.append(' ');
			String[] objectClasses = (String[]) sr.getProperty(Constants.OBJECTCLASS);
			if (isSpringApplicationContext(objectClasses)) {
				sb.append("{org.springframework.context.ApplicationContext}");
				Object symbolicName = sr.getProperty(Constants.BUNDLE_SYMBOLICNAME);
				if (symbolicName != null)
					sb.append(" " + Constants.BUNDLE_SYMBOLICNAME + ": " + symbolicName);
			} else {
				sb.append(arrayToString(objectClasses));
			}
			Object cn = sr.getProperty(NodeConstants.CN);
			if (cn != null)
				sb.append(" " + NodeConstants.CN + ": " + cn);
			Object factoryPid = sr.getProperty(ConfigurationAdmin.SERVICE_FACTORYPID);
			if (factoryPid != null)
				sb.append(" " + ConfigurationAdmin.SERVICE_FACTORYPID + ": " + factoryPid);
			// else {
			// Object servicePid = sr.getProperty(Constants.SERVICE_PID);
			// if (servicePid != null)
			// sb.append(" " + Constants.SERVICE_PID + ": " + servicePid);
			// }
			// servlets
			Object whiteBoardPattern = sr.getProperty(KernelConstants.WHITEBOARD_PATTERN_PROP);
			if (whiteBoardPattern != null) {
				if (whiteBoardPattern instanceof String) {
					sb.append(" " + KernelConstants.WHITEBOARD_PATTERN_PROP + ": " + whiteBoardPattern);
				} else {
					sb.append(" " + KernelConstants.WHITEBOARD_PATTERN_PROP + ": "
							+ arrayToString((String[]) whiteBoardPattern));
				}
			}
			// RWT
			Object contextName = sr.getProperty(KernelConstants.CONTEXT_NAME_PROP);
			if (contextName != null)
				sb.append(" " + KernelConstants.CONTEXT_NAME_PROP + ": " + contextName);

			// user directories
			Object baseDn = sr.getProperty(UserAdminConf.baseDn.name());
			if (baseDn != null)
				sb.append(" " + UserAdminConf.baseDn.name() + ": " + baseDn);

		}
		return sb.toString();
	}

	private String arrayToString(Object[] arr) {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0; i < arr.length; i++) {
			if (i != 0)
				sb.append(',');
			sb.append(arr[i]);
		}
		sb.append(']');
		return sb.toString();
	}

	private boolean isSpringApplicationContext(String[] objectClasses) {
		for (String clss : objectClasses) {
			if (clss.equals("org.eclipse.gemini.blueprint.context.DelegatedExecutionOsgiBundleApplicationContext")) {
				return true;
			}
		}
		return false;
	}

	//
	// ARGEO LOGGER
	//

	public synchronized void register(ArgeoLogListener listener, Integer numberOfPreviousEvents) {
		String username = CurrentUser.getUsername();
		if (username == null)
			throw new CmsException("Only authenticated users can register a log listener");

		if (!userListeners.containsKey(username)) {
			List<ArgeoLogListener> lst = Collections.synchronizedList(new ArrayList<ArgeoLogListener>());
			userListeners.put(username, lst);
		}
		userListeners.get(username).add(listener);
		List<LogEvent> lastEvents = logDispatcherThread.getLastEvents(username, numberOfPreviousEvents);
		for (LogEvent evt : lastEvents)
			dispatchEvent(listener, evt);
	}

	public synchronized void registerForAll(ArgeoLogListener listener, Integer numberOfPreviousEvents,
			boolean everything) {
		if (everything)
			everythingListeners.add(listener);
		else
			allUsersListeners.add(listener);
		List<LogEvent> lastEvents = logDispatcherThread.getLastEvents(null, numberOfPreviousEvents);
		for (LogEvent evt : lastEvents)
			if (everything || evt.getUsername() != null)
				dispatchEvent(listener, evt);
	}

	public synchronized void unregister(ArgeoLogListener listener) {
		String username = CurrentUser.getUsername();
		if (username == null)// FIXME
			return;
		if (!userListeners.containsKey(username))
			throw new CmsException("No user listeners " + listener + " registered for user " + username);
		if (!userListeners.get(username).contains(listener))
			throw new CmsException("No user listeners " + listener + " registered for user " + username);
		userListeners.get(username).remove(listener);
		if (userListeners.get(username).isEmpty())
			userListeners.remove(username);

	}

	public synchronized void unregisterForAll(ArgeoLogListener listener) {
		everythingListeners.remove(listener);
		allUsersListeners.remove(listener);
	}

	/** For development purpose, since using regular logging is not easy here */
	private static void stdOut(Object obj) {
		System.out.println(obj);
	}

	private static void stdErr(Object obj) {
		System.err.println(obj);
	}

	private static void debug(Object obj) {
		if (debug)
			System.out.println(obj);
	}

	private static boolean isInternalDebugEnabled() {
		return debug;
	}

	// public void setPattern(String pattern) {
	// this.pattern = pattern;
	// }

	public void setDisabled(Boolean disabled) {
		this.disabled = disabled;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public void setConfiguration(Properties configuration) {
		this.configuration = configuration;
	}

	public void updateConfiguration(Properties configuration) {
		setConfiguration(configuration);
		reloadConfiguration();
	}

	public Properties getConfiguration() {
		return configuration;
	}

	/**
	 * Reloads configuration (if the configuration {@link Properties} is set)
	 */
	protected void reloadConfiguration() {
		if (configuration != null) {
			LogManager.resetConfiguration();
			PropertyConfigurator.configure(configuration);
		}
	}

	protected synchronized void processLoggingEvent(LogEvent event) {
		if (disabled)
			return;

		if (dispatching.get())
			return;

		if (level != null && !level.trim().equals("")) {
			if (log4jLevel == null || !log4jLevel.toString().equals(level))
				try {
					log4jLevel = Level.toLevel(level);
				} catch (Exception e) {
					System.err.println("Log4j level could not be set for level '" + level + "', resetting it to null.");
					e.printStackTrace();
					level = null;
				}

			if (log4jLevel != null && !event.getLoggingEvent().getLevel().isGreaterOrEqual(log4jLevel)) {
				return;
			}
		}

		try {
			// admin listeners
			Iterator<ArgeoLogListener> everythingIt = everythingListeners.iterator();
			while (everythingIt.hasNext())
				dispatchEvent(everythingIt.next(), event);

			if (event.getUsername() != null) {
				Iterator<ArgeoLogListener> allUsersIt = allUsersListeners.iterator();
				while (allUsersIt.hasNext())
					dispatchEvent(allUsersIt.next(), event);

				if (userListeners.containsKey(event.getUsername())) {
					Iterator<ArgeoLogListener> userIt = userListeners.get(event.getUsername()).iterator();
					while (userIt.hasNext())
						dispatchEvent(userIt.next(), event);
				}
			}
		} catch (Exception e) {
			stdOut("Cannot process logging event");
			e.printStackTrace();
		}
	}

	protected void dispatchEvent(ArgeoLogListener logListener, LogEvent evt) {
		LoggingEvent event = evt.getLoggingEvent();
		logListener.appendLog(evt.getUsername(), event.getTimeStamp(), event.getLevel().toString(),
				event.getLoggerName(), event.getThreadName(), event.getMessage(), event.getThrowableStrRep());
	}

	private class AppenderImpl extends AppenderSkeleton {
		public boolean requiresLayout() {
			return false;
		}

		public void close() {
		}

		@Override
		protected void append(LoggingEvent event) {
			if (events != null) {
				try {
					String username = CurrentUser.getUsername();
					events.put(new LogEvent(username, event));
				} catch (InterruptedException e) {
					// silent
				}
			}
		}

	}

	private class LogDispatcherThread extends Thread {
		/** encapsulated in order to simplify concurrency management */
		private LinkedList<LogEvent> lastEvents = new LinkedList<LogEvent>();

		public LogDispatcherThread() {
			super("Argeo Logging Dispatcher Thread");
		}

		public void run() {
			while (events != null) {
				try {
					LogEvent loggingEvent = events.take();
					processLoggingEvent(loggingEvent);
					addLastEvent(loggingEvent);
				} catch (InterruptedException e) {
					if (events == null)
						return;
				}
			}
		}

		protected synchronized void addLastEvent(LogEvent loggingEvent) {
			if (lastEvents.size() >= maxLastEventsCount)
				lastEvents.poll();
			lastEvents.add(loggingEvent);
		}

		public synchronized List<LogEvent> getLastEvents(String username, Integer maxCount) {
			LinkedList<LogEvent> evts = new LinkedList<LogEvent>();
			ListIterator<LogEvent> it = lastEvents.listIterator(lastEvents.size());
			int count = 0;
			while (it.hasPrevious() && (count < maxCount)) {
				LogEvent evt = it.previous();
				if (username == null || username.equals(evt.getUsername())) {
					evts.push(evt);
					count++;
				}
			}
			return evts;
		}
	}

	private class LogEvent {
		private final String username;
		private final LoggingEvent loggingEvent;

		public LogEvent(String username, LoggingEvent loggingEvent) {
			super();
			this.username = username;
			this.loggingEvent = loggingEvent;
		}

		@Override
		public int hashCode() {
			return loggingEvent.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return loggingEvent.equals(obj);
		}

		@Override
		public String toString() {
			return username + "@ " + loggingEvent.toString();
		}

		public String getUsername() {
			return username;
		}

		public LoggingEvent getLoggingEvent() {
			return loggingEvent;
		}

	}

	private class Log4jConfWatcherThread extends Thread {
		private Path log4jConfigurationPath;

		public Log4jConfWatcherThread(Path log4jConfigurationPath) {
			super("Log4j Configuration Watcher");
			try {
				this.log4jConfigurationPath = log4jConfigurationPath.toRealPath();
			} catch (IOException e) {
				this.log4jConfigurationPath = log4jConfigurationPath.toAbsolutePath();
				stdOut("Cannot determine real path for " + log4jConfigurationPath + ": " + e.getMessage());
			}
		}

		public void run() {
			Path parentDir = log4jConfigurationPath.getParent();
			try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
				parentDir.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);
				WatchKey wk;
				watching: while ((wk = watchService.take()) != null) {
					for (WatchEvent<?> event : wk.pollEvents()) {
						final Path changed = (Path) event.context();
						if (log4jConfigurationPath.equals(parentDir.resolve(changed))) {
							if (isInternalDebugEnabled())
								debug(log4jConfigurationPath + " has changed, reloading.");
							PropertyConfigurator.configure(log4jConfigurationPath.toUri().toURL());
						}
					}
					// reset the key
					boolean valid = wk.reset();
					if (!valid) {
						break watching;
					}
				}
			} catch (IOException | InterruptedException e) {
				stdErr("Log4j configuration watcher failed: " + e.getMessage());
			}
		}
	}
}
