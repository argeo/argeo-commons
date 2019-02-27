package org.argeo.cms.ui.dialogs;

import org.argeo.cms.CmsMsg;
import org.argeo.cms.util.CmsUtils;
import org.argeo.eclipse.ui.EclipseUiUtils;
import org.argeo.eclipse.ui.Selected;
import org.argeo.eclipse.ui.dialogs.LightweightDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

public class CmsMessageDialog extends LightweightDialog {
	public final static int INFORMATION = 2;
	public final static int QUESTION = 3;
	public final static int WARNING = 4;
	public final static int CONFIRM = 5;

	private int kind;
	private String message;

	public CmsMessageDialog(Shell parentShell, String message, int kind) {
		super(parentShell);
		this.kind = kind;
		this.message = message;
	}

	protected Control createDialogArea(Composite parent) {
		parent.setLayout(new GridLayout());

		TraverseListener traverseListener = new TraverseListener() {
			private static final long serialVersionUID = -1158892811534971856L;

			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN)
					okPressed();
				else if (e.detail == SWT.TRAVERSE_ESCAPE)
					cancelPressed();
			}
		};

		// message
		Composite body = new Composite(parent, SWT.NONE);
		body.addTraverseListener(traverseListener);
		GridLayout bodyGridLayout = new GridLayout();
		bodyGridLayout.marginHeight = 20;
		bodyGridLayout.marginWidth = 20;
		body.setLayout(bodyGridLayout);
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label messageLbl = new Label(body, SWT.WRAP);
		CmsUtils.markup(messageLbl);
		messageLbl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		messageLbl.setFont(EclipseUiUtils.getBoldFont(parent));
		if (message != null)
			messageLbl.setText(message);

		// buttons
		Composite buttons = new Composite(parent, SWT.NONE);
		buttons.addTraverseListener(traverseListener);
		buttons.setLayoutData(new GridData(SWT.END, SWT.FILL, true, false));
		if (kind == INFORMATION || kind == WARNING) {
			GridLayout layout = new GridLayout(1, true);
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			buttons.setLayout(layout);

			Button close = new Button(buttons, SWT.FLAT);
			close.setText(CmsMsg.close.lead());
			close.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			close.addSelectionListener((Selected) (e) -> closeShell(OK));
			close.setFocus();
			close.addTraverseListener(traverseListener);

			buttons.setTabList(new Control[] { close });
		} else if (kind == CONFIRM || kind == QUESTION) {
			Control input = createInputArea(body);
			if (input != null) {
				input.addTraverseListener(traverseListener);
				body.setTabList(new Control[] { input });
			}
			GridLayout layout = new GridLayout(2, true);
			layout.marginWidth = 0;
			layout.marginHeight = 0;
			buttons.setLayout(layout);

			Button cancel = new Button(buttons, SWT.FLAT);
			cancel.setText(CmsMsg.cancel.lead());
			cancel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			cancel.addSelectionListener((Selected) (e) -> cancelPressed());
			cancel.addTraverseListener(traverseListener);

			Button ok = new Button(buttons, SWT.FLAT);
			ok.setText(CmsMsg.ok.lead());
			ok.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			ok.addSelectionListener((Selected) (e) -> okPressed());
			ok.addTraverseListener(traverseListener);
			if (input == null)
				ok.setFocus();
			else
				input.setFocus();

			buttons.setTabList(new Control[] { ok, cancel });
		}
		// pack();
		parent.setTabList(new Control[] { body, buttons });
		return body;
	}

	protected Control createInputArea(Composite parent) {
		return null;
	}

	protected void okPressed() {
		closeShell(OK);
	}

	protected void cancelPressed() {
		closeShell(CANCEL);
	}

	protected Point getInitialSize() {
		return new Point(400, 200);
	}

	public static boolean open(int kind, Shell parent, String message) {
		CmsMessageDialog dialog = new CmsMessageDialog(parent, message, kind);
		return dialog.open() == 0;
	}

	public static boolean openConfirm(String message) {
		return open(CONFIRM, Display.getCurrent().getActiveShell(), message);
	}

	public static void openInformation(String message) {
		open(INFORMATION, Display.getCurrent().getActiveShell(), message);
	}

	public static boolean openQuestion(String message) {
		return open(QUESTION, Display.getCurrent().getActiveShell(), message);
	}

	public static void openWarning(String message) {
		open(WARNING, Display.getCurrent().getActiveShell(), message);
	}

}