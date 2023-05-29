package org.argeo.init;

/** A runtime context with a life cycle. */
public interface RuntimeContext extends Runnable {
	/** Wait until this runtime context has closed. */
	void waitForStop(long timeout) throws InterruptedException;

	/** Close this runtime context. */
	void close() throws Exception;
}
