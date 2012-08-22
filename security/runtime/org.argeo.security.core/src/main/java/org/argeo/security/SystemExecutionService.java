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
package org.argeo.security;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;

/**
 * Allows to execute code authenticated as a system user (that is not a real
 * person). The {@link Executor} interface is not used directly in order to
 * allow future extension of this interface and to simplify its publication
 * (e.g. as an OSGi service) and interception.
 */
public interface SystemExecutionService extends Executor {
	/**
	 * Executes this {@link Runnable} within a system authenticated context.
	 * Implementations should make sure that this method is properly secured via
	 * Java permissions since it could access everything without credentials.
	 */
	public void execute(Runnable runnable);

	/**
	 * Executes this {@link Callable} within a system authenticated context.
	 * Implementations should make sure that this method is properly secured via
	 * Java permissions since it could access everything without credentials.
	 */
	public <T> Future<T> submit(Callable<T> task);
}
