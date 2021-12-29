package org.argeo.init;

public interface RuntimeContext extends Runnable, AutoCloseable {
	void waitForStop(long timeout) throws InterruptedException;
}
