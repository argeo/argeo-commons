package org.argeo.eclipse.ui.dialogs;

import org.eclipse.swt.widgets.Shell;

/**
 * Generic error dialog to be used in try/catch blocks
 * 
 * @deprecated use {@link org.argeo.eclipse.ui.Error} instead.
 */
public class Error extends org.argeo.eclipse.ui.Error {
	public Error(Shell parentShell, String message, Throwable e) {
		super(parentShell, message, e);
	}
}
