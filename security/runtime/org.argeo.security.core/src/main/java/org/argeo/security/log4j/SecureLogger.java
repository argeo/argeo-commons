/*
 * Copyright (C) 2010 Mathieu Baudier <mbaudier@argeo.org>
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

import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.apache.log4j.spi.LoggingEvent;
import org.argeo.ArgeoException;
import org.argeo.ArgeoLogListener;
import org.springframework.security.Authentication;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.anonymous.AnonymousAuthenticationToken;

/** Not meant to be used directly in standard log4j config */
public class SecureLogger {
	private List<ArgeoLogListener> listeners;

	private Boolean disabled = false;

	private String level = null;

	private Level log4jLevel = null;
	// private Layout layout;

	private Properties configuration;

	private AppenderImpl appender;

	/** Marker to prevent stack overflow */
	private ThreadLocal<Boolean> dispatching = new ThreadLocal<Boolean>() {

		@Override
		protected Boolean initialValue() {
			return false;
		}
	};

	public void init() {
		try {
			// if (layout != null)
			// setLayout(layout);
			// else
			// setLayout(new PatternLayout(pattern));
			appender = new AppenderImpl();

			if (configuration != null)
				PropertyConfigurator.configure(configuration);

			Logger.getRootLogger().addAppender(appender);
		} catch (Exception e) {
			throw new ArgeoException("Cannot initialize log4j");
		}
	}

	public void destroy() throws Exception {
		Logger.getRootLogger().removeAppender(appender);
	}

	// public void setLayout(Layout layout) {
	// this.layout = layout;
	// }

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

	public void setListeners(List<ArgeoLogListener> listeners) {
		this.listeners = listeners;
	}

	public void setConfiguration(Properties configuration) {
		this.configuration = configuration;
	}

	private class AppenderImpl extends AppenderSkeleton {
		public boolean requiresLayout() {
			return false;
		}

		public void close() {
		}

		@Override
		protected void append(LoggingEvent event) {
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
						&& !event.getLevel().isGreaterOrEqual(log4jLevel)) {
					return;
				}
			}

			try {
				Thread currentThread = Thread.currentThread();
				String username = null;

				// find username
				SecurityContext securityContext = SecurityContextHolder
						.getContext();
				if (securityContext != null) {
					Authentication authentication = securityContext
							.getAuthentication();
					if (authentication != null) {
						if (authentication instanceof AnonymousAuthenticationToken) {
							username = null;
						} else {
							username = authentication.getName();
						}
					}
				}

				// Spring OSGi safe
				Iterator<ArgeoLogListener> it = listeners.iterator();
				while (it.hasNext()) {
					ArgeoLogListener logListener = it.next();
					logListener.appendLog(username, event.getTimeStamp(), event
							.getLevel().toString(), event.getLoggerName(),
							currentThread.getName(), event.getMessage(), event.getThrowableStrRep());
				}
			} catch (Exception e) {
				stdOut("Cannot process logging event");
				e.printStackTrace();
			}
		}

	}
}
