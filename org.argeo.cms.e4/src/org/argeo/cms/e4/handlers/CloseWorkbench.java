package org.argeo.cms.e4.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.workbench.IWorkbench;

public class CloseWorkbench {
	@Execute
	public void execute(IWorkbench workbench) {
		workbench.close();
	}
}
