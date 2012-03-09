/*
 * Copyright (C) 2007-2012 Mathieu Baudier
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
package org.argeo.security.log4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggingEvent;
import org.argeo.ArgeoException;
import org.argeo.ArgeoLogListener;
import org.argeo.ArgeoLogger;
import org.argeo.security.SecurityUtils;

/** Not meant to be used directly in standard log4j config */
public class SecureLogger implements ArgeoLogger {

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
			throw new ArgeoException("Cannot initialize log4j");
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

	public synchronized void register(ArgeoLogListener listener,
			Integer numberOfPreviousEvents) {
		String username = SecurityUtils.getCurrentThreadUsername();
		if (username == null)
			throw new ArgeoException(
					"Only authenticated users can register a log listener");

		if (!userListeners.containsKey(username)) {
			List<ArgeoLogListener> lst = Collections
					.synchronizedList(new ArrayList<ArgeoLogListener>());
			userListeners.put(username, lst);
		}
		userListeners.get(username).add(listener);
		List<LogEvent> lastEvents = logDispatcherThread.getLastEvents(username,
				numberOfPreviousEvents);
		for (LogEvent evt : lastEvents)
			dispatchEvent(listener, evt);
	}

	public synchronized void registerForAll(ArgeoLogListener listener,
			Integer numberOfPreviousEvents, boolean everything) {
		if (everything)
			everythingListeners.add(listener);
		else
			allUsersListeners.add(listener);
		List<LogEvent> lastEvents = logDispatcherThread.getLastEvents(null,
				numberOfPreviousEvents);
		for (LogEvent evt : lastEvents)
			if (everything || evt.getUsername() != null)
				dispatchEvent(listener, evt);
	}

	public synchronized void unregister(ArgeoLogListener listener) {
		String username = SecurityUtils.getCurrentThreadUsername();
		if (!userListeners.containsKey(username))
			throw new ArgeoException("No user listeners " + listener
					+ " registered for user " + username);
		if (!userListeners.get(username).contains(listener))
			throw new ArgeoException("No user listeners " + listener
					+ " registered for user " + username);
		userListeners.get(username).remove(listener);
		if (userListeners.get(username).isEmpty())
			userListeners.remove(username);

	}

	public synchronized void unregisterForAll(ArgeoLogListener listener) {
		everythingListeners.remove(listener);
		allUsersListeners.remove(listener);
	}

	/** For development purpose, since using regular logging is not easy here */
	static void stdOut(Object obj) {
		System.out.println(obj);
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

	/** Reloads configuration (if the configuration {@link Properties} is set) */
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
					System.err
							.println("Log4j level could not be set for level '"
									+ level + "', resetting it to null.");
					e.printStackTrace();
					level = null;
				}

			if (log4jLevel != null
					&& !event.getLoggingEvent().getLevel()
							.isGreaterOrEqual(log4jLevel)) {
				return;
			}
		}

		try {
			// admin listeners
			Iterator<ArgeoLogListener> everythingIt = everythingListeners
					.iterator();
			while (everythingIt.hasNext())
				dispatchEvent(everythingIt.next(), event);

			if (event.getUsername() != null) {
				Iterator<ArgeoLogListener> allUsersIt = allUsersListeners
						.iterator();
				while (allUsersIt.hasNext())
					dispatchEvent(allUsersIt.next(), event);

				if (userListeners.containsKey(event.getUsername())) {
					Iterator<ArgeoLogListener> userIt = userListeners.get(
							event.getUsername()).iterator();
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
		logListener.appendLog(evt.getUsername(), event.getTimeStamp(), event
				.getLevel().toString(), event.getLoggerName(), event
				.getThreadName(), event.getMessage(), event
				.getThrowableStrRep());
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
					String username = SecurityUtils.getCurrentThreadUsername();
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

		public synchronized List<LogEvent> getLastEvents(String username,
				Integer maxCount) {
			LinkedList<LogEvent> evts = new LinkedList<LogEvent>();
			ListIterator<LogEvent> it = lastEvents.listIterator(lastEvents
					.size());
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
}
