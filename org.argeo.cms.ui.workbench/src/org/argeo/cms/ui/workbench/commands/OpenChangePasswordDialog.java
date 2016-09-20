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
package org.argeo.cms.ui.workbench.commands;

import static org.argeo.cms.CmsMsg.changePassword;
import static org.argeo.cms.CmsMsg.currentPassword;
import static org.argeo.cms.CmsMsg.newPassword;
import static org.argeo.cms.CmsMsg.passwordChanged;
import static org.argeo.cms.CmsMsg.repeatNewPassword;
import static org.eclipse.jface.dialogs.IMessageProvider.INFORMATION;

import java.security.AccessController;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import javax.transaction.UserTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.argeo.cms.CmsException;
import org.argeo.eclipse.ui.dialogs.ErrorFeedback;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.Dialog;
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
					passwordChanged.lead(), passwordChanged.lead());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	protected void changePassword(char[] oldPassword, char[] newPassword) {
		Subject subject = Subject.getSubject(AccessController.getContext());
		String name = subject.getPrincipals(X500Principal.class).iterator()
				.next().toString();
		LdapName dn;
		try {
			dn = new LdapName(name);
		} catch (InvalidNameException e) {
			throw new CmsException("Invalid user dn " + name, e);
		}
		User user = (User) userAdmin.getRole(dn.toString());
		if (!user.hasCredential(null, oldPassword))
			throw new CmsException("Invalid password");
		if (newPassword.equals(""))
			throw new CmsException("New password empty");
		try {
			userTransaction.begin();
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
				throw new CmsException("Cannot change password", e);
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
		private Text oldPassword, newPassword1, newPassword2;

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
			oldPassword = createLP(composite, currentPassword.lead());
			newPassword1 = createLP(composite, newPassword.lead());
			newPassword2 = createLP(composite, repeatNewPassword.lead());

			setMessage(changePassword.lead(), INFORMATION);
			parent.pack();
			return composite;
		}

		@Override
		protected void okPressed() {
			try {
				if (!newPassword1.getText().equals(newPassword2.getText()))
					throw new CmsException("New passwords are different");
				changePassword(oldPassword.getTextChars(),
						newPassword1.getTextChars());
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
			shell.setText(changePassword.lead());
		}

	}
}
