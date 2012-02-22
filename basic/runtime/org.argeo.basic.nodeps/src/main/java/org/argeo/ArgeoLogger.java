package org.argeo;

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
