package org.argeo.init.jshell;

import java.util.Map;

import jdk.jshell.execution.DirectExecutionControl;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionControlProvider;
import jdk.jshell.spi.ExecutionEnv;
import jdk.jshell.tool.JavaShellToolBuilder;

/**
 * Canonical {@link ExecutionControlProvider} wrapping a
 * {@link DirectExecutionControl}, so that {@link JavaShellToolBuilder} can run
 * in the same VM.
 */
public class DirectExecutionControlProvider implements ExecutionControlProvider {

	@Override
	public String name() {
		return "direct";
	}

	@Override
	public ExecutionControl generate(ExecutionEnv env, Map<String, String> parameters) throws Throwable {
		return new DirectExecutionControl();
	}

}
