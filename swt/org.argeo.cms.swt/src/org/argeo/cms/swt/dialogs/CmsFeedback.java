package org.argeo.cms.swt.dialogs;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import org.argeo.api.cms.CmsLog;
import org.argeo.cms.CmsMsg;
import org.argeo.cms.swt.Selected;
import org.argeo.cms.ux.widgets.CmsDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** A dialog feedback based on a {@link LightweightDialog}. */
public class CmsFeedback extends LightweightDialog {
	private final static CmsLog log = CmsLog.getLog(CmsFeedback.class);

	private String message;
	private Throwable exception;

	private Text stack;

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
			Display display = LightweightDialog.findDisplay();

			CmsFeedback current = (CmsFeedback) display.getData(CmsFeedback.class.getName());
			if (current != null) {// already one open
				current.append("");
				if (message != null)
					current.append(message);
				if (e != null)
					current.append(e);
				// FIXME set a limit to the size of the text
				return current;
			}

			CmsFeedback cmsFeedback = new CmsFeedback(null, message, e);
			cmsFeedback.setBlockOnOpen(false);
			cmsFeedback.open();
			cmsFeedback.getDisplay().setData(CmsFeedback.class.getName(), cmsFeedback);
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

	protected Control createDialogArea(Composite parent) {
		parent.setLayout(new GridLayout(2, false));

		Label messageLbl = new Label(parent, SWT.WRAP);
		messageLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (message != null)
			messageLbl.setText(message);
		else if (exception != null)
			messageLbl.setText(exception.getLocalizedMessage());

		Button close = new Button(parent, SWT.FLAT);
		close.setText(CmsMsg.close.lead());
		close.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));
		close.addSelectionListener((Selected) (e) -> closeShell(CmsDialog.OK));

		if (exception != null) {
			stack = new Text(parent, SWT.MULTI | SWT.LEAD | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			stack.setEditable(false);
			stack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
			append(exception);
		}
		return messageLbl;
	}

	protected Point getInitialSize() {
		if (exception != null)
			return new Point(800, 600);
		else
			return new Point(600, 400);
	}

	protected void append(String message) {
		stack.append(message);
		stack.append("\n");
	}

	protected void append(Throwable exception) {
		try (StringWriter sw = new StringWriter()) {
			exception.printStackTrace(new PrintWriter(sw));
			stack.append(sw.toString());
		} catch (IOException e) {
			// ignore
		}

	}

	@Override
	protected void onClose() {
		getDisplay().setData(CmsFeedback.class.getName(), null);
	}

}
