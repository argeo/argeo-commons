package org.argeo.cms.swt.dialogs;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.argeo.api.cms.CmsLog;
import org.argeo.cms.CmsMsg;
import org.argeo.cms.swt.Selected;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** A dialog feedback based on a {@link LightweightDialog}. */
public class CmsFeedback extends LightweightDialog {
	private final static CmsLog log = CmsLog.getLog(CmsFeedback.class);

	private String message;
	private Throwable exception;

	private CmsFeedback(Shell parentShell, String message, Throwable e) {
		super(parentShell);
		this.message = message;
		this.exception = e;
	}

	public static CmsFeedback error(String message, Throwable e) {
		// rethrow ThreaDeath in order to make sure that RAP will properly clean
		// up the UI thread
		if (e instanceof ThreadDeath)
			throw (ThreadDeath) e;

		log.error(message, e);
		try {
			CmsFeedback cmsFeedback = new CmsFeedback(null, message, e);
			cmsFeedback.setBlockOnOpen(false);
			cmsFeedback.open();
			return cmsFeedback;
		} catch (Throwable e1) {
			log.error("Cannot open error feedback (" + e.getMessage() + "), original error below", e);
			return null;
		}
	}

	public static CmsFeedback show(String message) {
		CmsFeedback cmsFeedback = new CmsFeedback(null, message, null);
		cmsFeedback.open();
		return cmsFeedback;
	}

	/** Tries to find a display */
	// private static Display getDisplay() {
	// try {
	// Display display = Display.getCurrent();
	// if (display != null)
	// return display;
	// else
	// return Display.getDefault();
	// } catch (Exception e) {
	// return Display.getCurrent();
	// }
	// }

	protected Control createDialogArea(Composite parent) {
		parent.setLayout(new GridLayout(2, false));

		Label messageLbl = new Label(parent, SWT.WRAP);
		if (message != null)
			messageLbl.setText(message);
		else if (exception != null)
			messageLbl.setText(exception.getLocalizedMessage());

		Button close = new Button(parent, SWT.FLAT);
		close.setText(CmsMsg.close.lead());
		close.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
		close.addSelectionListener((Selected) (e) -> closeShell(OK));

		// Composite composite = new Composite(dialogarea, SWT.NONE);
		// composite.setLayout(new GridLayout(2, false));
		// composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		if (exception != null) {
			Text stack = new Text(parent, SWT.MULTI | SWT.LEAD | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			stack.setEditable(false);
			stack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			StringWriter sw = new StringWriter();
			exception.printStackTrace(new PrintWriter(sw));
			stack.setText(sw.toString());
		}

		// parent.pack();
		return messageLbl;
	}

}
