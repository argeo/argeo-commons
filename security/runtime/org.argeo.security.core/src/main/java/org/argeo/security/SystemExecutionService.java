package org.argeo.security;

import org.springframework.core.task.TaskExecutor;

/**
 * Allows to execute code authenticated as a system user (that is not a real
 * person)
 */
public interface SystemExecutionService {
	/**
	 * Executes this Runnable within a system authenticated context.
	 * Implementations should make sure that this method is properly secured via
	 * Java permissions since it could access to everything without credentials.
	 */
	public void executeAsSystem(Runnable runnable);
	
	public TaskExecutor createSystemAuthenticatedTaskExecutor();
}
