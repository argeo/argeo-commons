package org.argeo.init;

import java.util.Map;

import org.argeo.api.init.RuntimeContext;

public class StaticRuntimeContext implements RuntimeContext {
	private Map<String, String> config;

	private boolean running = false;

	protected StaticRuntimeContext(Map<String, String> config) {
		this.config = config;
	}

	@Override
	public synchronized void run() {
		running = true;
		notifyAll();
	}

	@Override
	public void waitForStop(long timeout) throws InterruptedException {
		long begin = System.currentTimeMillis();
		while (running && (timeout == 0 || System.currentTimeMillis() - begin < timeout)) {
			synchronized (this) {
				wait(500);
			}
		}
	}

	@Override
	public synchronized void close() throws Exception {
		running = false;
	}

}
