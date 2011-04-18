package org.argeo.security.core;

import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.argeo.ArgeoException;
import org.argeo.security.SystemExecutionService;

/**
 * Implementation of a {@link SystemExecutionService} using a key-based
 * {@link InternalAuthentication}
 */
public class KeyBasedSystemExecutionService extends AbstractSystemExecution
		implements SystemExecutionService {
	public void execute(Runnable runnable) {
		try {
			wrapWithSystemAuthentication(Executors.callable(runnable)).call();
		} catch (Exception e) {
			throw new ArgeoException(
					"Exception when running system authenticated task", e);
		}
	}

	public <T> Future<T> submit(Callable<T> task) {
		FutureTask<T> future = new FutureTask<T>(
				wrapWithSystemAuthentication(task));
		future.run();
		return future;
	}

	protected <T> Callable<T> wrapWithSystemAuthentication(
			final Callable<T> runnable) {
		return new Callable<T>() {

			public T call() throws Exception {
				authenticateAsSystem();
				try {
					return runnable.call();
				} finally {
					deauthenticateAsSystem();
				}
			}
		};
	}
}
