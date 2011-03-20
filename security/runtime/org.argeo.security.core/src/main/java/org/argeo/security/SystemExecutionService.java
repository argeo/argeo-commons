package org.argeo.security;

import java.util.concurrent.Executor;

import org.springframework.core.task.TaskExecutor;

/**
 * Allows to execute code authenticated as a system user (that is not a real
 * person). The {@link Executor} interface interface is not used directly in
 * order to allow future extension of this interface and to simplify its
 * publication (e.g. as an OSGi service) and interception. Support for Spring's
 * {@link TaskExecutor} will be dropped when upgrading to Spring 3, since it is
 * only to ensure compatibility with versions of Java before 1.5.
 */
public interface SystemExecutionService extends Executor, TaskExecutor {
	/**
	 * Executes this Runnable within a system authenticated context.
	 * Implementations should make sure that this method is properly secured via
	 * Java permissions since it could access to everything without credentials.
	 */
	public void execute(Runnable runnable);
}
