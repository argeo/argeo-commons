package org.argeo.security;

import java.util.concurrent.Executor;

/**
 * Allows to execute code authenticated as a system user (that is not a real
 * person). The {@link Executor} interface interface is not used directly in
 * order to allow future extension of this interface and to simplify its
 * publication (e.g. as an OSGi service) and interception.
 */
public interface SystemExecutionService extends Executor {
	/**
	 * Executes this Runnable within a system authenticated context.
	 * Implementations should make sure that this method is properly secured via
	 * Java permissions since it could access to everything without credentials.
	 */
	public void execute(Runnable runnable);
}
