/*
 * Copyright (C) 2007-2012 Mathieu Baudier
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.argeo.security.ui.dialogs;

import org.argeo.ArgeoException;
import org.argeo.eclipse.ui.ErrorFeedback;
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
import org.springframework.security.userdetails.UserDetailsManager;

/** Dialog to change the current user password */
public class ChangePasswordDialog extends TitleAreaDialog {
	private Text currentPassword, newPassword1, newPassword2;
	private UserDetailsManager userDetailsManager;

	public ChangePasswordDialog(Shell parentShell,
			UserDetailsManager securityService) {
		super(parentShell);
		this.userDetailsManager = securityService;
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
		try {
			userDetailsManager.changePassword(currentPassword.getText(),
					newPassword1.getText());
			close();
		} catch (Exception e) {
			ErrorFeedback.show("Cannot change password", e);
		}
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
