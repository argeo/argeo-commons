package org.argeo.internal.init;

import org.argeo.api.init.RuntimeContext;

/**
 * Keep track of the internal state mostly with static variables, typically in
 * order to synchronise shutdown.
 */
public class InternalState {
	private static RuntimeContext mainRuntimeContext;

	/** The root runtime context in this JVM. */
	public static RuntimeContext getMainRuntimeContext() {
		return mainRuntimeContext;
	}

	public static void setMainRuntimeContext(RuntimeContext mainRuntimeContext) {
		InternalState.mainRuntimeContext = mainRuntimeContext;
	}

}
