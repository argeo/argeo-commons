package org.argeo.eclipse.ui;

import org.eclipse.swt.widgets.Shell;

/**
 * @deprecated deprecated because of poor naming, use {@link ErrorFeedback}
 *             instead
 */
@Deprecated
public class Error extends ErrorFeedback {

	public Error(Shell parentShell, String message, Throwable e) {
		super(parentShell, message, e);
		// TODO Auto-generated constructor stub
	}

}
