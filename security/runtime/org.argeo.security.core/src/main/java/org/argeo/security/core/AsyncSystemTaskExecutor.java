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
