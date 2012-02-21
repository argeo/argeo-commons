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
