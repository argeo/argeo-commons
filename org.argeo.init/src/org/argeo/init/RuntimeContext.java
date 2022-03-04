package org.argeo.init;

public interface RuntimeContext extends Runnable {
	void waitForStop(long timeout) throws InterruptedException;

	void close() throws Exception;
}
