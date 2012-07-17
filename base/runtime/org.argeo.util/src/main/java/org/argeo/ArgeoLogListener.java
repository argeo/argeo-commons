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
package org.argeo;

/** Framework agnostic interface for log notifications */
public interface ArgeoLogListener {
	/**
	 * Appends a log
	 * 
	 * @param username
	 *            authentified user, null for anonymous
	 * @param level
	 *            INFO, DEBUG, WARN, etc. (logging framework specific)
	 * @param category
	 *            hierarchy (logging framework specific)
	 * @param thread
	 *            name of the thread which logged this message
	 * @param msg
	 *            any object as long as its toString() method returns the
	 *            message
	 * @param the
	 *            exception in log4j ThrowableStrRep format
	 */
	public void appendLog(String username, Long timestamp, String level,
			String category, String thread, Object msg, String[] exception);
}
