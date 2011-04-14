package org.argeo.security.core;

import org.argeo.security.SystemExecutionService;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

/**
 * Asynchronous Spring TaskExecutor (for use in JMS for example) wrapping a
 * {@link SystemExecutionService}.
 */
public class AsyncSystemTaskExecutor extends SimpleAsyncTaskExecutor {
	private static final long serialVersionUID = -8035527542087963068L;

	private SystemExecutionService systemExecutionService;

	public AsyncSystemTaskExecutor() {
		super();
	}

	public AsyncSystemTaskExecutor(String threadNamePrefix) {
		super(threadNamePrefix);
	}

	@Override
	public Thread createThread(final Runnable runnable) {
		Runnable systemExecutionRunnable = new Runnable() {

			public void run() {
				systemExecutionService.execute(runnable);

			}
		};
		return super.createThread(systemExecutionRunnable);
	}

	public void setSystemExecutionService(
			SystemExecutionService systemExecutionService) {
		this.systemExecutionService = systemExecutionService;
	}

}
