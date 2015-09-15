/*
 * Copyright (C) 2007-2012 Argeo GmbH
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
package org.argeo.security.ui.commands;

import java.security.AccessController;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.ArgeoException;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.service.useradmin.User;
import org.osgi.service.useradmin.UserAdmin;

/** Opens the change password dialog. */
public class OpenChangePasswordDialog extends AbstractHandler {
	private final static Log log = LogFactory
			.getLog(OpenChangePasswordDialog.class);
	private UserAdmin userAdmin;
	private UserTransaction userTransaction;

	public Object execute(ExecutionEvent event) throws ExecutionException {
		ChangePasswordDialog dialog = new ChangePasswordDialog(
				HandlerUtil.getActiveShell(event), userAdmin);
		if (dialog.open() == Dialog.OK) {
			MessageDialog.openInformation(HandlerUtil.getActiveShell(event),
					"Password changed", "Password changed.");
		}
		return null;
	}

	protected void changePassword(char[] oldPassword, char[] newPassword) {
		Subject subject = Subject.getSubject(AccessController.getContext());
		String name = subject.getPrincipals(X500Principal.class).iterator()
				.next().toString();
		LdapName dn;
		try {
			dn = new LdapName(name);
		} catch (InvalidNameException e) {
			throw new ArgeoException("Invalid user dn " + name, e);
		}
		try {
			userTransaction.begin();
			User user = (User) userAdmin.getRole(dn.toString());
			if (user.hasCredential(null, oldPassword))
				user.getCredentials().put(null, newPassword);
			userTransaction.commit();
		} catch (Exception e) {
			try {
				userTransaction.rollback();
			} catch (Exception e1) {
				log.error("Could not roll back", e1);
			}
			if (e instanceof RuntimeException)
				throw (RuntimeException) e;
			else
				throw new ArgeoException("Cannot change password", e);
		}
	}

	public void setUserAdmin(UserAdmin userDetailsManager) {
		this.userAdmin = userDetailsManager;
	}

	public void setUserTransaction(UserTransaction userTransaction) {
		this.userTransaction = userTransaction;
	}

	class ChangePasswordDialog extends TitleAreaDialog {
		private static final long serialVersionUID = -6963970583882720962L;
		private Text currentPassword, newPassword1, newPassword2;

		public ChangePasswordDialog(Shell parentShell, UserAdmin securityService) {
			super(parentShell);
		}

		protected Point getInitialSize() {
			return new Point(400, 450);
		}

		protected Control createDialogArea(Composite parent) {
			Composite dialogarea = (Composite) super.createDialogArea(parent);
			dialogarea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					true));
			Composite composite = new Composite(dialogarea, SWT.NONE);
			composite.setLayout(new GridLayout(2, false));
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true,
					false));
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
				changePassword(currentPassword.getTextChars(),
						newPassword1.getTextChars());
				close();
			} catch (Exception e) {
				MessageDialog.openError(newPassword1.getShell(), "Error",
						"Cannot change password");
				e.printStackTrace();
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
}
