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
		} catch (RuntimeException e) {
			throw e;
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
//					deauthenticateAsSystem();
				}
			}
		};
	}
}
