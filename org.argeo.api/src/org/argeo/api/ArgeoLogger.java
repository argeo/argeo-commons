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
package org.argeo.api;

/**
 * Logging framework agnostic identifying a logging service, to which one can
 * register
 */
public interface ArgeoLogger {
	/**
	 * Register for events by threads with the same authentication (or all
	 * threads if admin)
	 */
	public void register(ArgeoLogListener listener,
			Integer numberOfPreviousEvents);

	/**
	 * For admin use only: register for all users
	 * 
	 * @param listener
	 *            the log listener
	 * @param numberOfPreviousEvents
	 *            the number of previous events to notify
	 * @param everything
	 *            if true even anonymous is logged
	 */
	public void registerForAll(ArgeoLogListener listener,
			Integer numberOfPreviousEvents, boolean everything);

	public void unregister(ArgeoLogListener listener);

	public void unregisterForAll(ArgeoLogListener listener);
}
