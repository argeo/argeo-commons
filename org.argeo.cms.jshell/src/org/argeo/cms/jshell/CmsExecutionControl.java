package org.argeo.cms.jshell;

import jdk.jshell.execution.DirectExecutionControl;
import jdk.jshell.execution.LoaderDelegate;
import jdk.jshell.spi.ExecutionControl;
import jdk.jshell.spi.ExecutionEnv;

/** Custom {@link ExecutionControl}. */
public class CmsExecutionControl extends DirectExecutionControl {
	private final ExecutionEnv executionEnv;

	public CmsExecutionControl(ExecutionEnv executionEnv, LoaderDelegate loaderDelegate) {
		super(loaderDelegate);
		this.executionEnv = executionEnv;

	}

	@Override
	protected void clientCodeEnter() throws InternalException {
		super.clientCodeEnter();
	}

	@Override
	protected void clientCodeLeave() throws InternalException {
		super.clientCodeLeave();
		executionEnv.userOut().flush();
	}

}
