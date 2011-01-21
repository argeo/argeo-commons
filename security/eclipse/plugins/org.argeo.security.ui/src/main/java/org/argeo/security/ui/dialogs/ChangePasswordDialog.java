package org.argeo.security.ui.dialogs;

import org.argeo.ArgeoException;
import org.argeo.security.ArgeoSecurityService;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/** Dialog to change the current user password */
public class ChangePasswordDialog extends TitleAreaDialog {
	private Text currentPassword, newPassword1, newPassword2;
	private ArgeoSecurityService securityService;

	public ChangePasswordDialog(Shell parentShell,
			ArgeoSecurityService securityService) {
		super(parentShell);
		this.securityService = securityService;
	}

	protected Point getInitialSize() {
		return new Point(300, 250);
	}

	protected Control createDialogArea(Composite parent) {
		Composite dialogarea = (Composite) super.createDialogArea(parent);
		dialogarea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite composite = new Composite(dialogarea, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		currentPassword = createLP(composite, "Current password");
		newPassword1 = createLP(composite, "New password");
		newPassword2 = createLP(composite, "Repeat new password");

		setMessage("Change password", IMessageProvider.INFORMATION);
		parent.pack();
		return composite;
	}

	@Override
	protected void okPressed() {
		if (!newPassword1.getText().equals(newPassword2.getText()))
			throw new ArgeoException("Passwords are different");
		securityService.updateCurrentUserPassword(currentPassword.getText(),
				newPassword1.getText());
		close();
	}

	/** Creates label and password. */
	protected Text createLP(Composite parent, String label) {
		new Label(parent, SWT.NONE).setText(label);
		Text text = new Text(parent, SWT.SINGLE | SWT.LEAD | SWT.PASSWORD
				| SWT.BORDER);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		return text;
	}

	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText("Change password");
	}

}
